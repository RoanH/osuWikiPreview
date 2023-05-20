package dev.roanh.isla.commands;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;

/**
 * Command to update the osu! wiki instance.
 * @author Roan
 */
public class OsuWiki extends Command{
	/**
	 * Path to the osu! web wiki.
	 */
	private static final File WIKI_PATH = new File("/home/roan/discord/Isla/osu-wiki");
	/**
	 * Path to the osu! web deploy key.
	 */
	private static final File AUTH_PATH = new File("/home/roan/discord/Isla/auth");
	/**
	 * HTTP client to use for requests.
	 */
	private static final HttpClient client = HttpClient.newBuilder().build();
	/**
	 * Wiki repository bound git instance.
	 */
	private static Git git;
	/**
	 * Cached list of remotes.
	 */
	private static Map<String, RemoteConfig> remotes = new HashMap<String, RemoteConfig>();
	/**
	 * The SSH connection to use.
	 */
	private static TransportConfigCallback transport;
	/**
	 * Lock to present simultaneous command runs.
	 */
	private volatile AtomicBoolean busy = new AtomicBoolean(false);
	/**
	 * Target ref for the last update.
	 */
	private String lastRef = null;
	
	/**
	 * Constructs a new osu! wiki command.
	 * @throws IOException When some IO exception occurs.
	 */
	public OsuWiki() throws IOException{
		super("wiki", "Update the osu! wiki / news preview site.", CommandPermission.forRole(1109514462794358815L), false);
		
		addOptionString("namespace", "The user or organisation the osu-wiki fork is under.", 100);
		addOptionString("ref", "The ref to switch to in the given name space (branch/hash/tag).", 100);
		
		git = Git.open(WIKI_PATH);
	}
	
	@Override
	public void execute(CommandMap args, CommandEvent original){
		if(busy.getAndSet(true)){
			original.reply("Already running an update, please try again later.");
			return;
		}
		
		original.deferReply(event->{
			try{
				switchBranch(args.get("namespace").getAsString(), args.get("ref").getAsString(), event);
			}catch(Throwable e){
				event.logError(e, "[OsuWiki] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}finally{
				busy.set(false);
			}
		});
	}
	
	/**
	 * Switch the site to the given ref from the given namespace.
	 * @param name The namespace for the ref (user / organisation).
	 * @param ref The reference to switch to.
	 * @param event The command event for progress updates.
	 * @throws Throwable When some exception occurs.
	 */
	private void switchBranch(String name, String ref, CommandEvent event) throws Throwable{
		String full = name + "/" + ref;
		boolean ff = full.equals(lastRef);
		
		ObjectId from;
		if(ff){
			from = getHead();
		}else{
			//copy the current state
			forcePush("wikisynccopy");
			
			//reset to ppy master
			forceFetch("ppy");
			reset("ppy", "master");
			forcePush("wikisync");
			from = getHead();
			
			//roll back the website
			updateWiki("wikisync", "wikisynccopy");
			
			lastRef = full;
		}
		
		//reset to the new branch
		findRemote(name);
		forceFetch(name);
		reset(name, ref);
		forcePush("wikisync");
		
		//update the website wiki
		ObjectId to = getHead();
		updateWiki(from.getName(), to.getName());
		
		//update the website news
		updateNews();
		
		event.replyEmbeds(buildDiff(name, ref, from, to, ff));
	}
	
	/**
	 * Builds an embed showing changes compared to the last time.
	 * @param name The namespace for the new ref.
	 * @param ref The new ref on the site.
	 * @param from The old ref.
	 * @param to The new ref.
	 * @param fastForward If the update was a fast forward instead of a reset.
	 * @return A message embed describing the update.
	 * @throws IOException When an IOException occurs.
	 * @throws GitAPIException When some git exception occurs.
	 */
	private static MessageEmbed buildDiff(String name, String ref, ObjectId from, ObjectId to, boolean fastForward) throws IOException, GitAPIException{
		Repository repo = git.getRepository();
		ObjectReader reader = repo.newObjectReader();

		CanonicalTreeParser oldTree = new CanonicalTreeParser();
		oldTree.reset(reader, from);

		CanonicalTreeParser newTree = new CanonicalTreeParser();
		newTree.reset(reader, to);

		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(new Color(255, 142, 230));
		embed.setAuthor("Ref: " + ref, "https://github.com/" + name + "/osu-wiki/tree/" + ref, null);
		embed.setFooter("HEAD: " + (fastForward ? (to.getName() + " (fast-forward)") : to.getName()));

		StringBuilder desc = embed.getDescriptionBuilder();
		for(DiffEntry item : git.diff().setOldTree(oldTree).setNewTree(newTree).setShowNameOnly(true).call()){
			if(item.getChangeType() != ChangeType.DELETE){
				int len = desc.length();
				desc.append("- [");
				desc.append(item.getNewPath());
				desc.append("](https://github.com/");
				desc.append(name);
				desc.append("/osu-wiki/blob/");
				desc.append(ref);
				desc.append('/');
				desc.append(item.getNewPath());
				desc.append(")\n");
				if(desc.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH - "_more_".length()){
					desc.delete(len, desc.length());
					desc.append("\n_more_");
					break;
				}
			}
		}

		return embed.build();
	}
	
	/**
	 * Updates all news articles.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private static void updateNews() throws IOException, InterruptedException{
		updateSite("news");
	}
	
	/**
	 * Updates wiki articles in the given ref range.
	 * @param from The starting ref.
	 * @param to The end ref.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private static void updateWiki(String from, String to) throws IOException, InterruptedException{
		updateSite(from + " " + to);
	}
	
	/**
	 * Runs a site command.
	 * @param command The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private static void updateSite(String command) throws IOException, InterruptedException{
		Builder request = HttpRequest.newBuilder();
		request = request.timeout(Duration.ofMinutes(10));
		request = request.uri(URI.create("http://192.168.2.19:8999/"));
		request = request.POST(BodyPublishers.ofString(command));
		
		HttpResponse<Void> resp = client.send(request.build(), BodyHandlers.discarding());
		if(resp.statusCode() != 200){
			throw new IOException("Status: " + resp.statusCode());
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
	 * Gets the SHA hash of the current HEAD ref.
	 * @return The HEAD SHA hash.
	 * @throws IOException When an IO exception occurs.
	 */
	private static ObjectId getHead() throws IOException{
		return git.getRepository().resolve("HEAD^{tree}");
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
	 * Finds the remote with the given name.
	 * @param name The name of the remote.
	 * @return The remote with the given name.
	 * @throws GitAPIException When a git exception occurs.
	 * @throws URISyntaxException When a URI is invalid.
	 */
	private static RemoteConfig findRemote(String name) throws GitAPIException, URISyntaxException{
		RemoteConfig remote = remotes.get(name);
		if(remote == null){
			remote = git.remoteList().call().stream().filter(r->r.getName().equals(name)).findFirst().orElse(null);
			if(remote == null){
				remote = git.remoteAdd().setName(name).setUri(new URIish("git@github.com:" + name + "/osu-wiki.git")).call();
			}
			
			remotes.put(name, remote);
		}
		
		return remote;
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
