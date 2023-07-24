package dev.roanh.wiki;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main controller for an osu! web instance.
 * @author Roan
 *
 */
public class OsuWeb{
	private final String domain;
	/**
	 * Lock to present simultaneous command runs.
	 */
	private AtomicBoolean busy = new AtomicBoolean(false);
	
	public OsuWeb(String domain){
		this.domain = domain;
	}
	
	public boolean tryLock(){
		return busy.getAndSet(true);
	}
	
	public void unlock(){
		busy.set(false);
	}
	
	public String getDomain(){
		return domain;
	}
	
	/**
	 * Updates all osu! web news articles.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void runNewsUpdate() throws InterruptedException, IOException{
		runArtisan("NewsPost::syncAll()");
	}
		
	/**
	 * Updates all osu! web wiki articles in the given ref range.
	 * @param from The current wiki ref.
	 * @param to The new wiki ref.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void runWikiUpdate(String from, String to) throws InterruptedException, IOException{
		runArtisan("OsuWiki::updateFromGithub(['before' => '" + from + "','after' => '" + to + "'])");
	}
	
	/**
	 * Runs an osu! web artisan command.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	protected void runArtisan(String cmd) throws InterruptedException, IOException{
		runCommand("docker exec -it osu-web-octane-deploy php artisan tinker --execute=\"" + cmd + "\"");
	}
	
	/**
	 * Runs a command to start the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void start() throws InterruptedException, IOException{
		//TODO unsupported for now
	}
	
	/**
	 * Runs a command to stop the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void stop() throws InterruptedException, IOException{
		//TODO unsupported for now
	}
	
	/**
	 * Runs a command on the server.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private void runCommand(String cmd) throws InterruptedException, IOException{
		new ProcessBuilder("bash", "-c", cmd).directory(new File("/home/roan/wiki/deploy")).inheritIO().start().waitFor();
	}
}
