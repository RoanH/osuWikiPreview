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

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.data.GroupSet;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.PullRequest;
import dev.roanh.wiki.data.User;
import dev.roanh.wiki.data.WebState;

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
	public static void init(Config config){
		executor = DBExecutors.newSingleThreadExecutor(config.getMainDatabaseContext(), "wiki");
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
		byte[] acl = instance.getAccessList() == null ? null : instance.getAccessList().encode();
		executor.insert(
			"REPLACE INTO instances (id, channel, port, `role`, tag, acl) VALUES (?, ?, ?, ?, ?, ?)",
			instance.getId(), instance.getChannel(), instance.getPort(), instance.getRoleId(), instance.getTag(), acl
		);
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
				rs.getLong("role"),
				rs.getString("tag"),
				rs.getBytes("acl")
			);
		});
	}
	
	public static void saveUserSession(UserExtended user, String sessionToken) throws DBException{
		final int groups = GroupSet.encodeGroups(user.getUserGroups()).encode();
		executor.insert(
			"INSERT INTO users (osu, username, `session`, `groups`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE username = ?, `session` = ?, `groups` = ?",
			user.getId(), user.getUsername(), sessionToken, user.getUsername(), groups, sessionToken, groups
		);
	}
	
	public static void updateUserDiscord(int user, long discord) throws DBException{
		executor.update("UPDATE users SET `discord` = ? WHERE osu = ?", discord, user);
	}
	
	public static void updateUserNameAndGroups(int user, String username, GroupSet groups) throws DBException{
		executor.update("UPDATE users SET username = ?, `groups` = ? WHERE osu = ?", username, groups.encode(), user);
	}
	
	public static User getUserBySession(String session) throws DBException{
		return executor.selectFirst("SELECT * FROM users WHERE `session` = ?", User::new, session).orElse(null);
	}
	
	/**
	 * Retrieves the users that have at least one from in the given set.
	 * @param groups The groups to check for.
	 * @return Users with one of the given groups.
	 * @throws DBException When a database exception occurs.
	 */
	public static List<User> getUsersWithGroup(GroupSet groups) throws DBException{
		return executor.selectAll("SELECT * FROM users WHERE (`groups` & ?) != 0", User::new, groups.encode());
	}
	
	public static User getUserById(int osuId) throws DBException{
		return executor.selectFirst("SELECT * FROM users WHERE `osu` = ?", User::new, osuId).orElse(null);
	}
	
	public static List<User> getUsers() throws DBException{
		return executor.selectAll("SELECT * FROM users", User::new);
	}
}
