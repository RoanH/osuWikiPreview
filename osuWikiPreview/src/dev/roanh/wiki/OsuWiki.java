/*
 * osu! wiki preview site
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev) and contributors.
 * GitHub Repository: https://github.com/RoanH/osuWikiPreview
 * GitLab Repository: https://git.roanh.dev/roan/osuwikipreview
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.roanh.wiki;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Command to update the osu! wiki instance.
 * @author Roan
 */
public class OsuWiki{
	/**
	 * Path to the osu! web wiki.
	 */
	private static final File WIKI_PATH = new File("/home/roan/wiki/osu-wiki");
	/**
	 * Path to the osu! web deploy key.
	 */
	private static final File AUTH_PATH = new File("/home/roan/wiki/auth");
	/**
	 * Wiki repository bound git instance.
	 */
	private static Git git;
	/**
	 * Cached list of remotes.
	 */
	private static Set<String> remotes = new CopyOnWriteArraySet<String>();
	/**
	 * Cached list of recent refs.
	 */
	private static Set<String> refs = new CopyOnWriteArraySet<String>();
	/**
	 * The SSH connection to use.
	 */
	private static TransportConfigCallback transport;
	
	/**
	 * Constructs a new osu! wiki command.
	 * @throws IOException When some IO exception occurs.
	 */
	public static void init() throws IOException{
		((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
		git = Git.open(WIKI_PATH);
	}
	
	/**
	 * Gets a list of recently used remotes.
	 * @return The recently used remotes.
	 */
	public static List<String> getRecentRemotes(){
		return new ArrayList<String>(remotes);
	}
	
	/**
	 * Gets a list of recently used refs.
	 * @return The recently used refs.
	 */
	public static List<String> getRecentRefs(){
		return new ArrayList<String>(refs);
	}
	
	/**
	 * Switch the site to the given ref from the given namespace.
	 * @param name The namespace for the ref (user / organisation).
	 * @param ref The reference to switch to.
	 * @param instance The osu! web instance to update with the changes.
	 * @return A record with change information about the switch.
	 * @throws Throwable When some exception occurs.
	 */
	public synchronized static SwitchResult switchBranch(String name, String ref, OsuWeb instance) throws Throwable{
		String full = name + "/" + ref;
		instance.setCurrentRef(full);
		refs.add(ref);
		
		//update master copy
		forceFetch("ppy");
		reset("ppy", "master");
		forcePush("master");
		ObjectId from = git.getRepository().resolve("origin/master");

		//reset to the new branch
		findRemote(name);
		forceFetch(name);
		reset(name, ref);
		forcePush("wikisync-" + instance.getID());
		
		//update the website wiki
		ObjectId to = getHead();
		instance.runWikiUpdate("master", "wikisync-" + instance.getID());
		
		//compute the diff
		List<DiffEntry> diff = computeDiff(from, to);
		
		//update the website news
		instance.runNewsUpdate(diff);
		
		//return the diff
		return new SwitchResult(diff, to.getName());
	}
	
	/**
	 * Builds an embed showing changed files between the given two refs. This function
	 * is equivalent to {@code git diff --diff-filter=d --name-only A...B} and in addition
	 * also only returns <code>.md</code> files.
	 * @param from The old ref.
	 * @param to The new ref.
	 * @return A list of changed files.
	 * @throws IOException When an IOException occurs.
	 * @throws GitAPIException When some git exception occurs.
	 */
	private static List<DiffEntry> computeDiff(ObjectId from, ObjectId to) throws IOException, GitAPIException{
		Repository repo = git.getRepository();
		try(ObjectReader reader = repo.newObjectReader(); RevWalk rev = new RevWalk(repo)){
			//attempt to find the merge base of both commits
			RevCommit target = repo.parseCommit(to);
			RevCommit source = repo.parseCommit(from);
			rev.markStart(source);
			rev.markStart(target);
			rev.setRetainBody(false);
			rev.setRevFilter(RevFilter.MERGE_BASE);
			
			//the merge base is the commit where the branches diverged and always an ancestor of both commits
			RevCommit base = rev.next();
			for(RevCommit parent : source.getParents()){
				if(parent.equals(base)){
					//if the source commit is an ancestor of the target commit the source commit is closer
					//in history to the target commit than the merge base which is a parent of the source commit
					base = source;
					break;
				}
			}

			//base tree
			CanonicalTreeParser oldTree = new CanonicalTreeParser();
			oldTree.reset(reader, base.getTree());
			
			//current head tree
			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, target.getTree());
			
			return git.diff().setOldTree(oldTree).setNewTree(newTree).setShowNameOnly(true).call().stream().filter(item->{
				return item.getChangeType() != ChangeType.DELETE && item.getNewPath().endsWith(".md");
			}).toList();
		}
	}
	
	/**
	 * Resets the current branch to the given state.
	 * @param name The namespace for the ref.
	 * @param ref The ref to reset to.
	 * @throws GitAPIException When a git exception occurs.
	 */
	private static void reset(String name, String ref) throws GitAPIException{
		git.reset().setMode(ResetType.HARD).setRef(name + "/" + ref).call();
	}
	
	/**
	 * Force pushes to the given target ref.
	 * @param targetRef The upstream ref to push to.
	 * @throws GitAPIException When a git exception occurs.
	 */
	private static void forcePush(String targetRef) throws GitAPIException{
		git.push().setTransportConfigCallback(transport).setRefSpecs(new RefSpec("wikisync:" + targetRef)).setForce(true).setRemote("origin").call();
	}
	
	/**
	 * Gets the current HEAD ref.
	 * @return The HEAD object.
	 * @throws IOException When an IO exception occurs.
	 */
	private static ObjectId getHead() throws IOException{
		return git.getRepository().resolve(Constants.HEAD);
	}
	
	/**
	 * Force fetch all refs from the given remote.
	 * @param remote The remote to fetch from.
	 * @throws InvalidRemoteException When the remote is invalid.
	 * @throws TransportException When something goes wrong during transport.
	 * @throws GitAPIException When a git exception occurs.
	 */
	private static void forceFetch(String remote) throws GitAPIException{
		git.fetch().setTransportConfigCallback(transport).setRemote(remote).setForceUpdate(true).setRemoveDeletedRefs(true).call();
	}
	
	/**
	 * Finds the remote with the given name or creates it.
	 * @param name The name of the remote.
	 * @throws GitAPIException When a git exception occurs.
	 * @throws URISyntaxException When a URI is invalid.
	 */
	private static void findRemote(String name) throws GitAPIException, URISyntaxException{
		if(!remotes.contains(name)){
			if(git.remoteList().call().stream().filter(r->r.getName().equals(name)).findFirst().isEmpty()){
				git.remoteAdd().setName(name).setUri(new URIish("git@github.com:" + name + "/osu-wiki.git")).call();
			}
			
			remotes.add(name);
		}
	}
	
	/**
	 * Record with information about a branch switch.
	 * @author Roan
	 * @param diff A diff with all changed files.
	 * @param head The new head commit hash.
	 * @see OsuWiki#switchBranch(String, String, OsuWeb)
	 */
	public static final record SwitchResult(List<DiffEntry> diff, String head){
	}

	static{
		SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder().setPreferredAuthentications("publickey").setHomeDirectory(AUTH_PATH).setSshDirectory(AUTH_PATH).build(null);
		transport = new TransportConfigCallback(){
			@Override
			public void configure(Transport transport){
				SshTransport sshTransport = (SshTransport)transport;
				sshTransport.setSshSessionFactory(sshSessionFactory);
			}
		};
	}
}
