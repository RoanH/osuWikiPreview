package dev.roanh.wiki.data;

/**
 * Constant information about a web instance.
 * @author Roan
 * @param id The ID of the instance.
 * @param channel The ID of the discord channel for this instance.
 */
public record Instance(int id, long channel){
	
	/**
	 * The name of the wiki preview sync branch for this instance.
	 * @return The wiki sync branch for this instance.
	 */
	public String getGitHubBranch(){
		return "wikisync-" + id;
	}
}
