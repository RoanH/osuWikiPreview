package dev.roanh.wiki.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import dev.roanh.infinity.util.Variable;
import dev.roanh.wiki.github.hooks.IssueCommentCreatedData;
import dev.roanh.wiki.github.obj.GitHubComment;
import dev.roanh.wiki.github.obj.GitHubIssue;
import dev.roanh.wiki.github.obj.GitHubIssuePullRequest;
import dev.roanh.wiki.github.obj.GitHubUser;
import dev.roanh.wiki.github.obj.IssueState;
import dev.roanh.wiki.github.obj.UserType;

public class WebHookHandlerTest extends WebhookTest{

	@Test
	public void commentEvent() throws Exception{
		Variable<IssueCommentCreatedData> value = new Variable<IssueCommentCreatedData>();
		CountDownLatch latch = new CountDownLatch(1);

		webhook.addIssueCommentHandler(data->{
			value.setValue(data);
			latch.countDown();
		});

		sendPullRequestCommentPayload();
		latch.await(10, TimeUnit.SECONDS);

		IssueCommentCreatedData data = value.getValue();
		assertNotNull(data);

		GitHubComment comment = data.comment();
		assertNotNull(comment);
		assertEquals(2480604277L, comment.id());
		assertEquals("Test 3", comment.body());

		GitHubUser commentUser = comment.user();
		assertNotNull(commentUser);
		assertEquals("RoanH", commentUser.login());
		assertEquals(8530896, commentUser.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", commentUser.avatarUrl());
		assertEquals(UserType.USER, commentUser.type());

		GitHubIssue issue = data.issue();
		assertEquals(2664436632L, issue.id());
		assertEquals(2, issue.number());
		assertEquals(IssueState.OPEN, issue.state());
		assertEquals("Preview Test", issue.title());
		assertTrue(issue.isPullRequest());

		GitHubIssuePullRequest pr = issue.pullRequest();
		assertNotNull(pr);
		assertNull(pr.mergedInstant());
		
		GitHubUser issueUser = comment.user();
		assertNotNull(issueUser);
		assertEquals("RoanH", issueUser.login());
		assertEquals(8530896, issueUser.id());
		assertEquals("https://avatars.githubusercontent.com/u/8530896?v=4", issueUser.avatarUrl());
		assertEquals(UserType.USER, issueUser.type());
	}
}
