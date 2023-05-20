package dev.roanh.isla.commands;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.permission.DevPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;

public class OsuWiki extends Command{
	private static final File WIKI_PATH = new File("/home/roan/discord/Isla/osu-wiki");
	private static final File AUTH_PATH = new File("/home/roan/discord/Isla/auth");
	/**
	 * HTTP client to use for requests.
	 */
	private static final HttpClient client = HttpClient.newBuilder().build();
	private static Git git;
	private static Map<String, RemoteConfig> remotes = new HashMap<String, RemoteConfig>();
	private static TransportConfigCallback transport;
	private volatile AtomicBoolean busy = new AtomicBoolean(false);
	
	public OsuWiki(String name, String description, CommandPermission permission, boolean guild) throws IOException{
		super(
			"wiki",
			"Update the osu! wiki / news preview site.",
			DevPermission.INSTANCE.or(//Roan
				CommandPermission.forUser(255090458332495872L),//Walavouchey
				CommandPermission.forUser(286947276826345472L),//RockRoller
				CommandPermission.forUser(495362111032131594L)//0x84f
			),
			false
		);
		
		addOptionString("namespace", "The user or organisation the osu-wiki fork is under.", 100);
		addOptionString("ref", "The ref to switch in the given name space (branch/hash/tag).", 100);
		
		git = Git.open(WIKI_PATH);
	}
	
	@Override
	public void execute(CommandMap args, CommandEvent event){
		if(busy.getAndSet(true)){
			event.reply("Already running an update, please try again later.");
			return;
		}
		
		try{
			event.reply("Starting site update...");
			switchBranch(args.get("namespace").getAsString(), args.get("ref").getAsString(), event);
		}catch(Throwable e){
			event.logError(e, "[OsuWiki] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
			event.sendChannel("An internal error occurred.");
		}finally{
			busy.set(false);
		}
	}
	
	//name - user/org, ref - branch name
	private static void switchBranch(String name, String ref, CommandEvent event) throws Throwable{
		//copy the current state
		event.sendChannel("Resetting branch...");
		forcePush("wikisynccopy");
		
		//reset to ppy master
		forceFetch("ppy");
		reset("ppy", "master");
		forcePush("wikisync");
		String from = getHead();
		
		//roll back the website
		event.sendChannel("Rolling back site...");
		updateWiki("wikisync", "wikisynccopy");
		
		//reset to the new branch
		event.sendChannel("Resetting to new ref...");
		findRemote(name);
		forceFetch(name);
		reset(name, ref);
		forcePush("wikisync");
		
		//update the website wiki
		event.sendChannel("Updating site wiki...");
		String to = getHead();
		updateWiki(from, to);
		
		//update the website news
		event.sendChannel("Updating site news...");
		updateNews();
		
		event.sendChannel("Done!");
	}
	
	private static void updateNews() throws IOException, InterruptedException{
		updateSite("news");
	}
	
	private static void updateWiki(String from, String to) throws IOException, InterruptedException{
		updateSite(from + " " + to);
	}
	
	private static void updateSite(String command) throws IOException, InterruptedException{
		Builder request = HttpRequest.newBuilder();
		request = request.timeout(Duration.ofMinutes(10));
		request = request.uri(URI.create("http://192.168.2.19/"));
		request = request.POST(BodyPublishers.ofString(command));
		if(client.send(request.build(), BodyHandlers.discarding()).statusCode() != 200){
			throw new IOException();
		}
	}
	
	private static void reset(String name, String ref) throws CheckoutConflictException, GitAPIException{
		git.reset().setMode(ResetType.HARD).setRef(name + "/" + ref).call();
	}
	
	private static void forcePush(String targetRef) throws InvalidRemoteException, TransportException, GitAPIException{
		git.push().setTransportConfigCallback(transport).setRefSpecs(new RefSpec("wikisync:" + targetRef)).setForce(true).setRemote("origin").call();
	}
	
	private static String getHead() throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException{
		return git.getRepository().resolve(Constants.HEAD).getName();
	}
	
	private static void forceFetch(String remote) throws InvalidRemoteException, TransportException, GitAPIException{
		git.fetch().setTransportConfigCallback(transport).setRemote(remote).setForceUpdate(true).setRemoveDeletedRefs(true).call();
	}
	
	private static RemoteConfig findRemote(String name) throws GitAPIException, URISyntaxException{//user to org
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
