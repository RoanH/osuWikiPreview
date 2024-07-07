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

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.wiki.db.Database;
import dev.roanh.wiki.db.RemoteDatabase;

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
	 * The connection to the database.
	 */
	private final Database database;
	/**
	 * Lock to prevent simultaneous command runs.
	 */
	private AtomicBoolean busy = new AtomicBoolean(false);
	/**
	 * Current state for this web instance.
	 */
	private WebState currentState = null;
	
	/**
	 * Constructs a new osu! web instance with the given domain.
	 * @param id Numerical ID used to identify this instance and associated services.
	 */
	public OsuWeb(int id){
		domain = "https://osu" + id + "." + Main.DOMAIN + "/";
		this.id = id;
		database = new RemoteDatabase(id);
	}
	
	/**
	 * Gets the current state of this web instance.
	 * @return The current state or null if nothing was
	 *         changed on this web instance yet.
	 * @see WebState
	 */
	public WebState getCurrentState(){
		return currentState;
	}
	
	/**
	 * Sets the current state for this instance.
	 * @param state The new state.
	 * @see WebState
	 */
	public void setCurrentState(WebState state){
		currentState = state;
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
	 * only be called after successfully locking this instance.
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
	 * @throws DBException When a database exception occurs.
	 */
	public void runNewsUpdate(List<DiffEntry> diff) throws InterruptedException, IOException, DBException{
		for(DiffEntry file : diff){
			clearNewsPost(file);
		}

		runArtisan("NewsPost::syncAll()");
		
		for(DiffEntry file : diff){
			fixLinks(file);
		}
	}
	
	/**
	 * Fixes some links in news articles. Currently only updates the parent
	 * for twitch embed links.
	 * @param news The news post to update.
	 * @throws DBException When a database exception occurs.
	 */
	public void fixLinks(DiffEntry news) throws DBException{
		String path = news.getNewPath();
		if(path.startsWith("news/")){
			database.runQuery("UPDATE news_posts SET page = REPLACE(page, \"parent=osu.ppy.sh\", \"parent=osu" + id + ".roanh.dev\") WHERE slug = ?", path.substring(path.lastIndexOf('/') + 1, path.length() - 3));
		}
	}
	
	/**
	 * Sets the published date for all future news posts to the current date.
	 * @throws DBException When a database exception occurs.
	 */
	public void redateNews() throws DBException{
		database.runQuery("UPDATE news_posts SET published_at = CURRENT_TIMESTAMP() WHERE published_at > CURRENT_TIMESTAMP()");
	}
	
	/**
	 * Clear all data in the news posts database.
	 * @throws DBException When a database exception occurs.
	 */
	public void clearNewsDatabase() throws DBException{
		database.runQuery("DELETE FROM news_posts");
	}
	
	/**
	 * Clears all data for the given news post from the database.
	 * @param news The news post to remove.
	 * @throws DBException When a database exception occurs.
	 */
	public void clearNewsPost(DiffEntry news) throws DBException{
		String path = news.getNewPath();
		if(path.startsWith("news/")){
			database.runQuery("DELETE FROM news_posts WHERE slug = ?", path.substring(path.lastIndexOf('/') + 1, path.length() - 3));
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
	 * @throws DBException When a database exception occurs.
	 */
	public void start() throws InterruptedException, IOException, DBException{
		database.init();
		runCommand("docker start osu-web-redis-" + id + " osu-web-elasticsearch-" + id + " osu-web-" + id);
	}
	
	/**
	 * Stops the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 * @throws DBException When a database exception occurs.
	 */
	public void stop() throws InterruptedException, IOException, DBException{
		runCommand("docker stop osu-web-redis-" + id + " osu-web-elasticsearch-" + id + " osu-web-" + id);
		database.shutdown();
	}
	
	/**
	 * Runs an osu! web artisan command.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void runArtisan(String cmd) throws InterruptedException, IOException{
		runCommand("docker exec -it osu-web-" + id + " php artisan tinker --execute=\"" + cmd + "\"");
	}
	
	/**
	 * Runs a command on the server.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	public void runCommand(String cmd) throws InterruptedException, IOException{
		if(0 != new ProcessBuilder("bash", "-c", cmd).directory(Main.DEPLOY_PATH).inheritIO().start().waitFor()){
			throw new IOException("Executed command returned a non-zero exit code.");
		}
	}
}
