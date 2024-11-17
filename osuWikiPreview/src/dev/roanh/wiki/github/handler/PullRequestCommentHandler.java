package dev.roanh.wiki.github.handler;

import java.io.IOException;

import dev.roanh.wiki.github.hooks.IssueCommentData;

@FunctionalInterface
public abstract interface PullRequestCommentHandler{

	public abstract void handleComment(IssueCommentData data) throws IOException;
}
