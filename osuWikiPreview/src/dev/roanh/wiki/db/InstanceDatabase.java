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
package dev.roanh.wiki.db;

import dev.roanh.infinity.db.DBContext;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;

/**
 * Database connection implementation for osu! web.
 * @author Roan
 */
public class InstanceDatabase{//TODO just move into osuweb?
	/**
	 * The database connection.
	 */
	private final DBExecutorService executor;
	
	/**
	 * Constructs a new remote database.
	 * @param url The database connection URL without schema.
	 * @param pass The database password.
	 * @param id The preview instance ID.
	 */
	public InstanceDatabase(String url, String pass, int id){
		executor = DBExecutors.newSingleThreadExecutor(new DBContext(url + "osu" + id, "osuweb", pass), "wiki" + id);
	}
	
	/**
	 * Closes the connection with the database.
	 * @throws DBException When a database exception occurs.
	 */
	public void shutdown() throws DBException{
		executor.shutdown();
	}
	
	/**
	 * Runs the given SQL query on the database for this instance.
	 * @param query The query to execute.
	 * @throws DBException When a database exception occurs.
	 */
	public void runQuery(String query) throws DBException{
		executor.update(query);
	}
	
	/**
	 * Runs the given SQL query on the database for this instance.
	 * The query is allowed to have a single string parameter.
	 * @param query The query to execute.
	 * @param param The query string parameter.
	 * @throws DBException When a database exception occurs.
	 */
	public void runQuery(String query, String param) throws DBException{
		executor.update(query, param);
	}
}
