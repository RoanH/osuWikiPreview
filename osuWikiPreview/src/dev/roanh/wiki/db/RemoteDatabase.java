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

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.db.concurrent.DBExecutorService;
import dev.roanh.infinity.db.concurrent.DBExecutors;
import dev.roanh.wiki.Main;

public class RemoteDatabase implements Database{
	private final int id;
	private DBExecutorService executor;
	
	public RemoteDatabase(int id){
		this.id = id;
	}
	
	@Override
	public void init() throws DBException{
		Configuration config = Main.client.getConfig();
		executor = DBExecutors.newSingleThreadExecutor(new DBContext(config.readString("db-url") + id, "osuweb", config.readString("db-pass")), "wiki");
	}

	@Override
	public void runQuery(String query) throws DBException{
		executor.update(query);
	}

	@Override
	public void runQuery(String query, String param) throws DBException{
		executor.update(query, param);
	}
	
	@Override
	public void shutdown() throws DBException{
		executor.shutdown();
	}
}
