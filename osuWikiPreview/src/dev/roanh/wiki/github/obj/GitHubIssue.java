package dev.roanh.wiki.github.obj;

import com.google.gson.annotations.SerializedName;

public record GitHubIssue(
		long id,
		int number,
		IssueState state,
		String title,
		@SerializedName("pull_request")
		GitHubIssuePullRequest pullRequest
		//TODO user?
	){
	
	public boolean isPullRequest(){
		return pullRequest != null;
	}
}
