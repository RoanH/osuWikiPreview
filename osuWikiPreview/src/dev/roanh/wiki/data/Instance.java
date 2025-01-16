package dev.roanh.wiki.data;

import dev.roanh.wiki.Main;

/**
 * Constant information about a web instance.
 * @author Roan
 * @param id The ID of the instance.
 * @param channel The ID of the discord channel for this instance.
 * @param port The external docker container port for the website.
 */
public record Instance(int id, long channel, int port){
	
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
	
	public String getEnvFile(){
		return "osu" + id + ".env";
	}
	
	public String getElasticsearchPrefix(){
		return "osu" + id;
	}
}
