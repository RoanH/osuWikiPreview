package dev.roanh.wiki.github.obj;

public record GitHubPullRequest(
		long id,
		int number,
		GitHubUser user,
		String title//,
		//head/ref
		//head/repo/{name,full_name}
	){

}
