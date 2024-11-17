package dev.roanh.wiki.github.hooks;

import dev.roanh.wiki.github.obj.GitHubComment;
import dev.roanh.wiki.github.obj.GitHubIssue;

//prs are also issues
public record IssueCommentData(String action, GitHubComment comment, GitHubIssue issue){
	
	public boolean isCreateAction(){
		return "create".equals(action);
	}
	
	//action = created | deleted | edited

}
