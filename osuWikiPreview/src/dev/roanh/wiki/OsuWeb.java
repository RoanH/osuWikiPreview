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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.diff.DiffEntry;

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.exception.WebException;

/**
 * Main controller for an osu! web instance.
 * @author Roan
 */
public class OsuWeb{
	/**
	 * The instance this osu! web instance is associated with.
	 */
	private final Instance instance;
	/**
	 * The osu! web database connection for this instance.
	 */
	private final DBExecutorService executor;
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
	 * @param config The general configuration file.
	 * @param instance The instance for this osu! web instance.
	 */
	public OsuWeb(Configuration config, Instance instance){
		this.instance = instance;
		executor = DBExecutors.newSingleThreadExecutor(new DBContext(config.readString("db-url") + instance.getDatabaseSchemaPrefix(), "osuweb", config.readString("db-pass")), "wiki" + instance.getId());
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
	 * @throws DBException When a database exception occurs.
	 * @see WebState
	 */
	public void setCurrentState(WebState state) throws DBException{
		currentState = state;
		MainDatabase.saveState(instance.getId(), currentState);
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
	 * Gets the GitHub branched used by this instance.
	 * @return The GitHub sync branch for this instance.
	 */
	public String getWikiSyncBranch(){
		return instance.getGitHubBranch();
	}
	
	/**
	 * Gets the instance for this osu! web instance.
	 * @return The instance for this web instance.
	 */
	public Instance getInstance(){
		return instance;
	}
	
	/**
	 * Constructs a manager for this instance.
	 * @return The manager for this instance.
	 */
	public InstanceManager getManager(){
		return new InstanceManager(instance);
	}
	
	/**
	 * Updates all osu! web news articles.
	 * @param diff A diff indicating repository files that were changed.
	 * @throws DBException When a database exception occurs.
	 * @throws WebException When an exception occurs.
	 */
	public void runNewsUpdate(List<DiffEntry> diff) throws DBException, WebException{
		for(DiffEntry file : diff){
			clearNewsPost(file);
		}

		syncAllNews();
		
		for(DiffEntry file : diff){
			fixLinks(file);
		}
	}
	
	/**
	 * Sync's all news posts.
	 * @throws WebException When an exception occurs.
	 */
	public void syncAllNews() throws WebException{
		runArtisan("NewsPost::syncAll()");
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
			executor.update("UPDATE news_posts SET page = REPLACE(page, \"parent=osu.ppy.sh\", \"parent=" + instance.getDomain() + "\") WHERE slug = ?", path.substring(path.lastIndexOf('/') + 1, path.length() - 3));
		}
	}
	
	/**
	 * Sets the published date for all future news posts to the current date.
	 * @throws DBException When a database exception occurs.
	 */
	public void redateNews() throws DBException{
		executor.update("UPDATE news_posts SET published_at = CURRENT_TIMESTAMP() WHERE published_at > CURRENT_TIMESTAMP()");
	}
	
	/**
	 * Clear all data in the news posts database.
	 * @throws DBException When a database exception occurs.
	 */
	public void clearNewsDatabase() throws DBException{
		executor.delete("DELETE FROM news_posts");
	}
	
	/**
	 * Clears all data for the given news post from the database.
	 * @param news The news post to remove.
	 * @throws DBException When a database exception occurs.
	 */
	public void clearNewsPost(DiffEntry news) throws DBException{
		String path = news.getNewPath();
		if(path.startsWith("news/")){
			executor.delete("DELETE FROM news_posts WHERE slug = ?", path.substring(path.lastIndexOf('/') + 1, path.length() - 3));
		}
	}
		
	/**
	 * Updates all osu! web wiki articles in the given ref range.
	 * @param from The current wiki ref.
	 * @param to The new wiki ref.
	 * @throws WebException When an exception occurs.
	 */
	public void runWikiUpdate(String from, String to) throws WebException{
		runArtisan("OsuWiki::updateFromGithub(['before' => '" + from + "','after' => '" + to + "'])");
	}
	
	/**
	 * Starts the osu! web instance.
	 * @throws DBException When a database exception occurs.
	 * @throws WebException When an exception occurs.
	 */
	public void start() throws DBException, WebException{
		currentState = MainDatabase.getState(instance.getId());
		Main.runCommand("docker start " + instance.getWebContainer());
	}
	
	/**
	 * Stops the osu! web instance.
	 * @throws DBException When a database exception occurs.
	 * @throws WebException When an exception occurs.
	 */
	public void stop() throws DBException, WebException{
		Main.runCommand("docker stop " + instance.getWebContainer());
	}
	
	/**
	 * Runs an osu! web artisan command.
	 * @param cmd The command to run.
	 * @throws WebException When an exception occurs.
	 */
	public void runArtisan(String cmd) throws WebException{
		Main.runCommand("docker exec -t " + instance.getWebContainer() + " php artisan tinker --execute=\"" + cmd + "\"");
	}
}
