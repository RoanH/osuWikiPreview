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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
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
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.LoggerFactory;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.wiki.exception.MergeConflictException;
import dev.roanh.wiki.exception.WebException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Command to update the osu! wiki instance.
 * @author Roan
 */
public class OsuWiki{
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
		git = Git.open(Main.WIKI_PATH);
		git.getRepository().getConfig().setBoolean("commit", null, "gpgsign", false);
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
	
	public synchronized static SwitchResult pushNews(byte[] data, int year, String filename, OsuWeb instance) throws GitAPIException, IOException, DBException, WebException{
		//update master copy
		ObjectId from = updateMaster();
		
		//write & commit file
		String path = "news/" + year + "/" + filename;
		Files.write(Main.WIKI_PATH.toPath().resolve(path), data);
		
		//commit file
		commitNewsFile(path);
		
		return pushBranch(from, instance);
	}
	
	/**
	 * Switch the site to the given ref from the given namespace.
	 * @param name The namespace for the ref (user / organisation).
	 * @param ref The reference to switch to.
	 * @param mergeMaster Whether to merge ppy/master into the ref before updating the site.
	 * @param instance The osu! web instance to update with the changes.
	 * @return A record with change information about the switch.
	 * @throws MergeConflictException If a merge with master is requested but a conflict occurs.
	 * @throws IOException 
	 * @throws GitAPIException 
	 * @throws WebException 
	 * @throws DBException 
	 */
	public synchronized static SwitchResult switchBranch(String name, String ref, boolean mergeMaster, OsuWeb instance) throws MergeConflictException, GitAPIException, IOException, DBException, WebException{
		refs.add(ref);
		
		//update master copy
		ObjectId from = updateMaster();

		//reset to the new branch
		findRemote(name);
		forceFetch(name);
		reset(name, ref);
		
		//merge changes from master if requested
		if(mergeMaster){
			mergeMaster(from);
		}
		
		return pushBranch(from, instance);
	}
	
	private static SwitchResult pushBranch(ObjectId from, OsuWeb instance) throws IOException, GitAPIException, DBException, WebException{
		//push the new state to the remote
		forcePush("wikisync-" + instance.getID());

		//update the website wiki
		ObjectId to = getHead();
		instance.runWikiUpdate("master", "wikisync-" + instance.getID());

		//compute the diff
		SwitchResult diff = new SwitchResult(computeDiff(from, to), to.getName());

		//update the website news
		if(diff.hasNews()){
			instance.runNewsUpdate(diff.diff());
		}

		//return the diff
		return diff;
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
			rev.setRetainBody(false);
			RevCommit target = rev.parseCommit(to);
			RevCommit source = rev.parseCommit(from);
			rev.markStart(source);
			rev.markStart(target);
			rev.setRevFilter(RevFilter.MERGE_BASE);
			
			//merge base tree
			CanonicalTreeParser oldTree = new CanonicalTreeParser();
			oldTree.reset(reader, rev.next().getTree());
			
			//current head tree
			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, target.getTree());
			
			return git.diff().setOldTree(oldTree).setNewTree(newTree).setShowNameOnly(true).call().stream().filter(item->{
				return item.getChangeType() != ChangeType.DELETE && item.getNewPath().endsWith(".md");
			}).toList();
		}
	}
	
	private static RevCommit commitNewsFile(String path) throws GitAPIException{
		return git.commit().setCommitter("Roan Hofland", "roan@roanh.dev").setMessage("Add newspost").setOnly(path).call();
	}
	
	/**
	 * Merges the local copy of ppy/master into the currently checked out branch.
	 * @param master A reference to the head of the current master branch.
	 * @throws GitAPIException When some exception occurs.
	 * @throws MergeConflictException When the merge fails due to a merge conflict.
	 */
	private static void mergeMaster(ObjectId master) throws GitAPIException, MergeConflictException{
		if(!git.merge().include(master).setCommit(true).setMessage("Merge ppy/master").setFastForward(FastForwardMode.NO_FF).setContentMergeStrategy(ContentMergeStrategy.CONFLICT).call().getMergeStatus().isSuccessful()){
			throw new MergeConflictException();
		}
	}
	
	/**
	 * Updates the local and remote copy of ppy/master with the latest changes.
	 * @return A reference to the current head of the master branch.
	 * @throws GitAPIException When some exception occurs.
	 * @throws IOException When an IOException occurs.
	 */
	private static ObjectId updateMaster() throws GitAPIException, IOException{
		forceFetch("ppy");
		reset("ppy", "master");
		forcePush("master");
		return git.getRepository().resolve("origin/master");
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
	 */
	private static void findRemote(String name) throws GitAPIException{
		try{
			if(!remotes.contains(name)){
				if(git.remoteList().call().stream().filter(r->r.getName().equals(name)).findFirst().isEmpty()){
					git.remoteAdd().setName(name).setUri(new URIish("git@github.com:" + name + "/osu-wiki.git")).call();
				}
				
				remotes.add(name);
			}
		}catch(URISyntaxException ignore){
			throw new InvalidRemoteException(name);
		}
	}
	
	/**
	 * Record with information about a branch switch.
	 * @author Roan
	 * @param diff A diff with all changed files.
	 * @param head The new head commit hash.
	 * @see OsuWiki#switchBranch(String, String, boolean, OsuWeb)
	 */
	public static final record SwitchResult(List<DiffEntry> diff, String head){
		
		/**
		 * Tests if there are news post items in this diff.
		 * @return True if this diff contains news posts.
		 */
		public boolean hasNews(){
			return diff.stream().map(DiffEntry::getNewPath).anyMatch(p->p.startsWith("news/"));
		}
	}
	
	static{
		SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder().setPreferredAuthentications("publickey").setHomeDirectory(Main.AUTH_PATH).setSshDirectory(Main.AUTH_PATH).build(null);
		transport = transport->{
			SshTransport sshTransport = (SshTransport)transport;
			sshTransport.setSshSessionFactory(sshSessionFactory);
		};
	}
}
