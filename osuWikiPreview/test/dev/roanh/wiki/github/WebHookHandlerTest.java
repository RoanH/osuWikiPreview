package dev.roanh.wiki.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import dev.roanh.infinity.util.Variable;
import dev.roanh.wiki.github.hooks.IssueCommentData;
import dev.roanh.wiki.github.obj.GitHubComment;
import dev.roanh.wiki.github.obj.GitHubIssue;
import dev.roanh.wiki.github.obj.GitHubIssuePullRequest;
import dev.roanh.wiki.github.obj.GitHubUser;
import dev.roanh.wiki.github.obj.IssueCommentActionType;
import dev.roanh.wiki.github.obj.IssueState;

public class WebHookHandlerTest extends WebhookTest{

	@Test
	public void commentEvent() throws Exception{
		Variable<IssueCommentData> value = new Variable<IssueCommentData>();
		CountDownLatch latch = new CountDownLatch(1);

		webhook.addIssueCommentHandler(data->{
			value.setValue(data);
			latch.countDown();
		});

		sendPullRequestCommentPayload();
		latch.await(10, TimeUnit.SECONDS);

		IssueCommentData data = value.getValue();
		assertNotNull(data);
		assertEquals(IssueCommentActionType.CREATED, data.action());

		GitHubComment comment = data.comment();
		assertNotNull(comment);
		assertEquals("Test 3", comment.body());
		assertEquals(2480604277L, comment.id());

		GitHubUser user = comment.user();
		assertNotNull(user);
		assertEquals("RoanH", user.login());
		assertEquals(8530896, user.id());

		GitHubIssue issue = data.issue();
		assertEquals(2664436632L, issue.id());
		assertEquals(2, issue.number());
		assertEquals(IssueState.OPEN, issue.state());
		assertEquals("Preview Test", issue.title());
		assertTrue(issue.isPullRequest());

		GitHubIssuePullRequest pr = issue.pullRequest();
		assertNotNull(pr);
		assertNull(pr.mergedInstant());
	}
}
