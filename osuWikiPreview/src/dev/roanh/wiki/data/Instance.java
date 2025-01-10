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
	 * Gets the base URL this instance can be browsed at.
	 * @return The URL for this instance.
	 */
	public String getBaseUrl(){
		return "https://" + getDomain() + "/";
	}
	
	public String getDomain(){
		return "osu" + id + "." + Main.DOMAIN;
	}
	
	public String getWebContainer(){
		return "osu-web-" + id;
	}
}
