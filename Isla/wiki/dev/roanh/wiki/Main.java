package dev.roanh.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

/**
 * Standalone webserver to receive commands and run commands.
 * @author Roan
 */
public class Main{
	
	/**
	 * Starts the webserver.
	 * @param args Command line arguments.
	 * @throws IOException When an IOException occurs.
	 */
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
	
	/**
	 * Updates all osu! web news articles.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private static void runNewsUpdate() throws InterruptedException, IOException{
		runArtisan("NewsPost::syncAll()");
	}
	
	/**
	 * Updates all osu! web wiki articles in the given ref range.
	 * @param from The current wiki ref.
	 * @param to The new wiki ref.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private static void runWikiUpdate(String from, String to) throws InterruptedException, IOException{
		runArtisan("OsuWiki::updateFromGithub(['before' => '" + from + "','after' => '" + to + "'])");
	}
	
	/**
	 * Runs an osu! web artisan command.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
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
