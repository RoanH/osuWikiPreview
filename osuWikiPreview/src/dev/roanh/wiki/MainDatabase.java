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

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;
import dev.roanh.wiki.data.Instance;

/**
 * Main database for the application (not instance specific).
 * @author Roan
 */
public final class MainDatabase{
	/**
	 * The database connection.
	 */
	private static DBExecutorService executor;

	/**
	 * Prevent instantiation.
	 */
	private MainDatabase(){
	}
	
	/**
	 * Initialises the connection with the database.
	 * @param config The application configuration.
	 */
	public static void init(Configuration config){
		executor = DBExecutors.newSingleThreadExecutor(new DBContext(config.readString("db-url") + "wikipreview", "osuweb", config.readString("db-pass")), "wiki");
	}
	
	/**
	 * Saves the current state of the web instance with the given ID.
	 * @param id The ID of the web instance to save the state of.
	 * @param state The state of the web instance.
	 * @throws DBException When a database exception occurs.
	 */
	public static void saveState(int id, WebState state) throws DBException{
		PullRequest pr = state.getPullRequest().orElse(new PullRequest(-1L, -1));
		executor.insert(
			"REPLACE INTO state (id, namespace, ref, redate, master, pr_id, pr_num, available) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
			id, state.getNamespace(), state.getRef(), state.hasRedate(), state.hasMaster(), pr.id(), pr.number(), state.getAvailableAt().getEpochSecond()
		);
	}
	
	/**
	 * Retrieves the last known state of the web instance with the given ID.
	 * @param id The ID of the web instance to retrieve the state of.
	 * @return The last known state of the given instance or null if not known.
	 * @throws DBException When a database exception occurs.
	 */
	public static WebState getState(int id) throws DBException{
		return executor.selectFirst("SELECT * FROM state WHERE id = ?", WebState::new, id).orElse(null);
	}
	
	/**
	 * Registers a new osu! web instance.
	 * @param instance The new instance to register.
	 * @throws DBException When a database exception occurs.
	 */
	public static void saveInstance(Instance instance) throws DBException{
		executor.insert("REPLACE INTO instances (id, channel, port, tag) VALUES (?, ?, ?, ?)", instance.getId(), instance.getChannel(), instance.getPort(), instance.getTag());
	}
	
	/**
	 * Gets a list of all registered osu! web instances.
	 * @return A list of all osu! web instances.
	 * @throws DBException When a database exception occurs.
	 */
	public static List<Instance> getInstances() throws DBException{
		return executor.selectAll("SELECT * FROM instances", rs->{
			return new Instance(
				rs.getInt("id"),
				rs.getLong("channel"),
				rs.getInt("port"),
				rs.getString("tag")
			);
		});
	}
}
