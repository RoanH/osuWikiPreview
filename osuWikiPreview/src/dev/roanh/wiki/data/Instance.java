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
 */
public class Instance{
	/**
	 * The ID of the instance.
	 */
	private final int id;
	/**
	 * The ID of the discord channel for this instance.
	 */
	private final long channel;
	/**
	 * The external docker container port for the website.
	 */
	private final int port;
	/**
	 * The osu! web docker image tag.
	 */
	private String tag;
	
	/**
	 * Constructs a new instance.
	 * @param id The ID of the instance.
	 * @param channel The discord channel for the instance.
	 * @param port The port for the website.
	 * @param tag The docker image tag.
	 */
	public Instance(int id, long channel, int port, String tag){
		this.id = id;
		this.channel = channel;
		this.port = port;
		this.tag = tag;
	}
	
	/**
	 * Gets the ID of the instance.
	 * @return The instance ID.
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * Gets the ID of the discord channel for this instance.
	 * @return The ID of the discord channel.
	 */
	public long getChannel(){
		return channel;
	}
	
	/**
	 * Gets the external docker container port for the website.
	 * @return The website port for the instance.
	 */
	public int getPort(){
		return port;
	}
	
	/**
	 * Gets the osu! web docker image tag for this instance.
	 * @return The osu! web docker tag.
	 */
	public String getTag(){
		return tag;
	}
	
	/**
	 * Sets a new osu! web docker release image tag for this instance.
	 * @param tag The new osu! web docker image tag.
	 */
	public void setTag(String tag){
		this.tag = tag;
	}
	
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
		return "osu" + id + "." + Main.config.domain();
	}
	
	/**
	 * Gets the name of the osu! web docker container for this instance.
	 * @return The name of the osu! web docker container.
	 */
	public String getWebContainer(){
		return "osu-web-" + id;
	}
	
	/**
	 * Gets the name of the osu! web database schema prefix for this instance.
	 * @return The main schema name and prefix for other schema names.
	 */
	public String getDatabaseSchemaPrefix(){
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
