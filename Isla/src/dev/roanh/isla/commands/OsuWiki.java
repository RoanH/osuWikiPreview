package dev.roanh.isla.commands;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

public class OsuWiki{
	private static final Path WIKI_PATH = Paths.get("C:\\Users\\roanh\\Downloads\\osu-wiki");
	private static Git git;
	private static Map<String, RemoteConfig> remotes = new HashMap<String, RemoteConfig>();
	
	
	
	public static void main(String[] args) throws IOException{
		git = Git.open(WIKI_PATH.toFile());
		
		
		SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
	        .setPreferredAuthentications("publickey")
	        .setHomeDirectory(new File("C:\\Users\\roanh\\Downloads\\auth"))
	        .setSshDirectory(new File("C:\\Users\\roanh\\Downloads\\auth"))
	        .build(null);
		

			TransportConfigCallback transport = new TransportConfigCallback() {
			  @Override
			  public void configure( Transport transport ) {
			    SshTransport sshTransport = ( SshTransport )transport;
			    sshTransport.setSshSessionFactory( sshSessionFactory );
			  }
			} ;
		
		
		
//		C:\Users\roanh\Downloads\osu-wiki
		
		
		try{
			String name = "Walavouchey";
			String ref = "action-multiline-colour-test";
			
			RemoteConfig remote = findRemote(name);
			git.fetch().setTransportConfigCallback(transport).setRemote(name).setForceUpdate(true).setRemoveDeletedRefs(true).call();
			
			
			git.reset().setMode(ResetType.HARD).setRef(name + "/" + ref).call();
			
			git.push().setTransportConfigCallback(transport).add("wikisync").setForce(true).setRemote("origin").call();
			
//			git.fetch().setRemote("origin").setForceUpdate(true).setRemoveDeletedRefs(true).setRefSpecs("news").call();

		}catch(GitAPIException | URISyntaxException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
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
