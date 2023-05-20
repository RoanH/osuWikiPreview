package dev.roanh.isla.commands;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

public class OsuWiki{
	private static final Path WIKI_PATH = Paths.get("C:\\Users\\roanh\\Downloads\\osu-wiki");
	/**
	 * HTTP client to use for requests.
	 */
	private static final HttpClient client = HttpClient.newBuilder().build();
	private static Git git;
	private static Map<String, RemoteConfig> remotes = new HashMap<String, RemoteConfig>();
	private static TransportConfigCallback transport;
	
	
	public static void main(String[] args) throws IOException, RevisionSyntaxException, InvalidRemoteException, TransportException, GitAPIException, URISyntaxException, InterruptedException{
		git = Git.open(WIKI_PATH.toFile());
		
		
		SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
	        .setPreferredAuthentications("publickey")
	        .setHomeDirectory(new File("C:\\Users\\roanh\\Downloads\\auth"))
	        .setSshDirectory(new File("C:\\Users\\roanh\\Downloads\\auth"))
	        .build(null);
		

			 transport = new TransportConfigCallback() {
			  @Override
			  public void configure( Transport transport ) {
			    SshTransport sshTransport = ( SshTransport )transport;
			    sshTransport.setSshSessionFactory( sshSessionFactory );
			  }
			} ;
		
		
		

			
			
		switchBranch("RoanH", "testb");
		
	}
	
	//name - user/org, ref - branch name
	private static void switchBranch(String name, String ref) throws InvalidRemoteException, TransportException, GitAPIException, RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException, URISyntaxException, InterruptedException{
		//copy the current state
		forcePush("wikisynccopy");
		
		//reset to ppy master
		forceFetch("ppy");
		reset("ppy", "master");
		forcePush("wikisync");
		String from = getHead();
		
		//roll back the website
		updateSite("wikisync", "wikisynccopy");
		
		//reset to the new branch
		findRemote(name);
		forceFetch(name);
		reset(name, ref);
		forcePush("wikisync");
		
		//update the website
		String to = getHead();
		updateSite(from, to);
		
	}
	
	private static void updateSite(String from, String to) throws IOException, InterruptedException{
		Builder request = HttpRequest.newBuilder();
		request = request.timeout(Duration.ofMinutes(10));
		request = request.uri(URI.create("http://192.168.2.19/"));
		request = request.POST(BodyPublishers.ofString(from + " " + to));
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
}
