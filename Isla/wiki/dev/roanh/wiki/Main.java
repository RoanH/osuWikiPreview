package dev.roanh.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

public class Main{
	
	public static void main(String[] args) throws IOException{
		HttpServer server = HttpServer.create(new InetSocketAddress(8999), 1);
		server.createContext("/", req->{
			if(req.getRequestMethod().equals("POST")){
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(req.getRequestBody(), StandardCharsets.UTF_8))){
					String arg = reader.readLine();
					if(arg.equals("news")){
						runNewsUpdate();
					}else{
						String[] refs = arg.split(" ");
						runWikiUpdate(refs[0], refs[1]);
					}
					
					req.sendResponseHeaders(200, 0);
					req.close();
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		});
		server.start();
	}
	
	private static void runNewsUpdate() throws InterruptedException, IOException{
		runArtisan("NewsPost::syncAll()");
	}
	
	private static void runWikiUpdate(String from, String to) throws InterruptedException, IOException{
		runArtisan("OsuWiki::updateFromGithub(['before' => '" + from + "','after' => '" + to + "'])");
	}
	
	private static void runArtisan(String cmd) throws InterruptedException, IOException{
		new ProcessBuilder(
			"docker",
			"compose",
			"exec",
			"php",
			"/app/docker/development/entrypoint.sh",
			"artisan",
			"tinker",
			"--execute=\"" + cmd + "\""
		).directory(new File("/home/roan/osu-web")).inheritIO().start().waitFor();
	}
}
