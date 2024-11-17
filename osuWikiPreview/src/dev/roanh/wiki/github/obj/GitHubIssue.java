package dev.roanh.wiki.github.obj;

import com.google.gson.annotations.SerializedName;

public record GitHubIssue(
		long id,
		int number,
		String state,
		String title,
		@SerializedName("pull_request")
		GitHubIssuePullRequest pullRequest){
	
	public boolean isPullRequest(){
		return pullRequest != null;
	}
	
	//state = open | closed

}
