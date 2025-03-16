package dev.roanh.wiki;

/**
 * Basic GitHub pull request identification.
 * @author Roan
 * @param id The globally unique ID of the pull request.
 * @param number The repository specific pull request number.
 */
public record PullRequest(long id, int number){

	/**
	 * Gets the complete web URL for this PR assuming it is in the official repository.
	 * @return The PR web URL.
	 */
	public String getPrLink(){
		return "https://github.com/ppy/osu-wiki/pull/" + number;
	}
}
