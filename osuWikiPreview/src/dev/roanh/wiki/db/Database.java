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

import dev.roanh.infinity.db.concurrent.DBException;

public abstract interface Database{
	
	public abstract void init() throws DBException;
	
	public abstract void shutdown() throws DBException;
	
	
	/**
	 * Runs the given SQL query on the database for this instance.
	 * @param query The query to execute.
	 * @throws DBException When a database exception occurs.
	 */
	public abstract void runQuery(String query) throws DBException;
	
	/**
	 * Runs the given SQL query on the database for this instance.
	 * The query is allowed to have a single string parameter.
	 * @param query The query to execute.
	 * @throws DBException When a database exception occurs.
	 */
	public abstract void runQuery(String query, String param) throws DBException;
}
