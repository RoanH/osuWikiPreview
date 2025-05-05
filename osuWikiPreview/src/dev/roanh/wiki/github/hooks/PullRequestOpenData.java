package dev.roanh.wiki.github.hooks;

import dev.roanh.wiki.github.obj.GitHubPullRequest;

public record PullRequestOpenData(GitHubPullRequest pullRequest){
}
