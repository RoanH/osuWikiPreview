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

import java.util.regex.Pattern;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.data.WebState;
import dev.roanh.wiki.exception.WebException;

/**
 * Local docker image MySQL instance connection.
 * @author Roan
 */
public class DockerDatabase implements Database{
	/**
	 * Regex allowing only valid slug characters.
	 */
	private static final Pattern SLUG_REGEX = Pattern.compile("[-0-9a-zA-Z]+");
	/**
	 * The osu! web instance.
	 */
	private final OsuWeb web;
	
	/**
	 * Constructs a new docker image database with the given web instance.
	 * @param web The associated osu! web instance.
	 */
	public DockerDatabase(OsuWeb web){
		this.web = web;
	}
	
	@Override
	public void runQuery(String query) throws DBException{
		try{
			web.runCommand("docker exec -it osu-web-mysql-" + web.getID() + " mysql osu -e \"" + query + ";\"");
		}catch(WebException ignore){
			throw new DBException(ignore);
		}
	}

	@Override
	public void runQuery(String query, String param) throws DBException{
		if(!SLUG_REGEX.matcher(param).matches()){
			throw new IllegalArgumentException("Unsafe param: " + param);
		}
		
		runQuery(query.replace("?", "'" + param + "'"));
	}

	@Override
	public void init() throws DBException{
		try{
			web.runCommand("docker start osu-web-mysql-" + web.getID());
		}catch(WebException ignore){
			throw new DBException(ignore);
		}
	}

	@Override
	public void shutdown() throws DBException{
		try{
			web.runCommand("docker stop osu-web-mysql-" + web.getID());
		}catch(WebException ignore){
			throw new DBException(ignore);
		}
	}
	
	@Override
	public WebState getState(int id) throws DBException{
		//unsupported, just revert to a clean state
		return null;
	}
	
	@Override
	public void saveState(int id, WebState state) throws DBException{
		//unsupported
	}
}
