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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main controller for an osu! web instance.
 * @author Roan
 */
public class OsuWeb{
	/**
	 * The domain this instance can be browsed at.
	 */
	private final String domain;
	/**
	 * Numerical ID of this instance to identify associated docker containers.
	 */
	private final int id;
	/**
	 * Lock to present simultaneous command runs.
	 */
	private AtomicBoolean busy = new AtomicBoolean(false);
	/**
	 * Ref currently checked out by this instance.
	 */
	private String currentRef = null;
	
	/**
	 * Constructs a new osu! web instance with the given domain.
	 * @param id Numerical ID used to identify this instance and associated services.
	 */
	public OsuWeb(int id){
		domain = "https://osu" + id + "." + Main.DOMAIN + "/";
		this.id = id;
	}
	
	/**
	 * Sets the current ref for this instance.
	 * @param ref The new reference.
	 */
	public void setCurrentRef(String ref){
		currentRef = ref;
	}
	
	/**
	 * Checks if switching this instance to checkout the given
	 * ref would be a fast-forward operation.
	 * @param targetRef The target ref.
	 * @return True if checking out the target ref would be a fast-forward.
	 */
	public boolean isFastFoward(String targetRef){
		return targetRef.equals(currentRef);
	}
	
	/**
	 * Attempts to lock this instance for exclusive use.
	 * @return True if instance was already locked and thus
	 *         should not be used.
	 * @see #unlock()
	 */
	public boolean tryLock(){
		return busy.getAndSet(true);
	}
	
	/**
	 * Unlocks this instance for use by other threads, should
	 * only be called after succesfully locking this instance.
	 * @see #tryLock()
	 */
	public void unlock(){
		busy.set(false);
	}
	
	/**
	 * Gets the website domain for this instance.
	 * @return The domain for this instance.
	 */
	public String getDomain(){
		return domain;
	}
	
	/**
	 * Gets the ID for this osu! web instance.
	 * @return The ID for this instance.
	 */
	public int getID(){
		return id;
	}
	
	/**
	 * Updates all osu! web news articles.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void runNewsUpdate() throws InterruptedException, IOException{
		runQuery("DELETE FROM news_posts");
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
	 * Starts the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void start() throws InterruptedException, IOException{
		runCommand("docker start osu-web-mysql-" + id + " osu-web-redis-" + id + " osu-web-elasticsearch-" + id + " osu-web-" + id);
	}
	
	/**
	 * Stops the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void stop() throws InterruptedException, IOException{
		runCommand("docker stop osu-web-mysql-" + id + " osu-web-redis-" + id + " osu-web-elasticsearch-" + id + " osu-web-" + id);
	}
	
	/**
	 * Runs an osu! web artisan command.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	protected void runArtisan(String cmd) throws InterruptedException, IOException{
		runCommand("docker exec -it osu-web-" + id + " php artisan tinker --execute=\"" + cmd + "\"");
	}
	
	/**
	 * Runs the given SQL query on the database for this instance.
	 * @param query The query to execute.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	protected void runQuery(String query) throws InterruptedException, IOException{
		runCommand("docker exec -it osu-web-mysql-" + id + " mysql osu -e \"" + query + ";\"");
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
