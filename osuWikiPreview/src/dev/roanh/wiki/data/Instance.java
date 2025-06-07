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
package dev.roanh.wiki.data;

import dev.roanh.wiki.Main;

/**
 * Constant information about a web instance.
 * @author Roan
 * @param id The ID of the instance.
 * @param channel The ID of the discord channel for this instance.
 * @param port The external docker container port for the website.
 * @param tag The osu-web docker image tag.
 */
public record Instance(int id, long channel, int port, String tag){
	
	/**
	 * The name of the wiki preview sync branch for this instance.
	 * @return The wiki sync branch for this instance.
	 */
	public String getGitHubBranch(){
		return "wikisync-" + id;
	}
	
	/**
	 * Gets the URL this instance can be browsed at.
	 * @return The URL for this instance.
	 */
	public String getSiteUrl(){
		return "https://" + getDomain();
	}
	
	/**
	 * Gets the website domain for this instance.
	 * @return The website domain.
	 */
	public String getDomain(){
		return "osu" + id + "." + Main.DOMAIN;
	}
	
	/**
	 * Gets the name of the osu! web docker container for this instance.
	 * @return The name of the osu! web docker container.
	 */
	public String getWebContainer(){
		return "osu-web-" + id;
	}
	
	/**
	 * Gets the name of the main osu! web database schema for this instance.
	 * @return The main schema name.
	 */
	public String getDatabaseSchema(){
		return "osu" + id;
	}
	
	/**
	 * Gets the name of the environment configuration file for this instance.
	 * @return The name of the environment configuration file for this instance.
	 */
	public String getEnvFile(){
		return "osu" + id + ".env";
	}
	
	/**
	 * Gets the Elasticsearch prefixed used by this instance.
	 * @return The Elasticsearch prefix used by this instance.
	 */
	public String getElasticsearchPrefix(){
		return "osu" + id;
	}
}
