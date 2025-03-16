package dev.roanh.wiki;

public record PullRequest(long id, int number){

	/**
	 * Gets the complete web URL for this PR assuming it is in the official repository.
	 * @return The PR web URL.
	 */
	public String getPrLink(){
		return "https://github.com/ppy/osu-wiki/pull/" + number;
	}
}
