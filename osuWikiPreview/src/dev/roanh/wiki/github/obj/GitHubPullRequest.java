package dev.roanh.wiki.github.obj;

public record GitHubPullRequest(
		long id,
		int number,
		IssueState state,
		String title,
		GitHubUser user,
		String body,
		GitHubBranch head
	){
}
