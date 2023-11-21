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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.diff.DiffEntry;

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
	 * Gets the ref currently checked out on this instance.
	 * <p>
	 * Format: <code>namespace/ref</code>
	 * @return The current ref.
	 */
	public String getCurrentRef(){
		return currentRef;
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
	 * @param diff A diff indicating repository files that were changed.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void runNewsUpdate(List<DiffEntry> diff) throws InterruptedException, IOException{
		for(DiffEntry file : diff){
			clearNewsPost(file);
		}

		runArtisan("NewsPost::syncAll()");
	}
	
	/**
	 * Sets the published date for all future news posts to the current date.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void redateNews() throws InterruptedException, IOException{
		runQuery("UPDATE news_posts SET published_at = CURRENT_TIMESTAMP() WHERE published_at > CURRENT_TIMESTAMP()");
	}
	
	/**
	 * Clear all data in the news posts database.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void clearNewsDatabase() throws InterruptedException, IOException{
		runQuery("DELETE FROM news_posts");
	}
	
	/**
	 * Clears all data for the given news post from the database.
	 * @param news The news post to remove.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void clearNewsPost(DiffEntry news) throws InterruptedException, IOException{
		String path = news.getNewPath();
		if(path.startsWith("news/")){
			String slug = path.substring(path.lastIndexOf('/') + 1, path.length() - 3);
			if(slug.matches("[-0-9a-zA-Z]+")){
				runQuery("DELETE FROM news_posts WHERE slug = '" + slug + "'");
			}
		}
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
		if(0 != new ProcessBuilder("bash", "-c", cmd).directory(Main.DEPLOY_PATH).inheritIO().start().waitFor()){
			throw new IOException("Executed command returned a non-zero exit code.");
		}
	}
}
