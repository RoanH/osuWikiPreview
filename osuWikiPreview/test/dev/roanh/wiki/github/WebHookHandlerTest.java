package dev.roanh.wiki.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class WebHookHandlerTest extends WebhookTest{

	@Test
	public void commentEvent() throws Exception{
		Variable<IssueCommentData> var = new Variable<IssueCommentData>();
		CountDownLatch latch = new CountDownLatch(1);

		webhook.addPullRequestCommentHandler(data->{
			var.setValue(data);
			latch.countDown();
		});

		sendPullRequestCommentPayload();
		latch.await(10, TimeUnit.SECONDS);

		IssueCommentData data = var.getValue();
		assertNotNull(data);
		assertTrue(data.isCreateAction());

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
		assertTrue(issue.isOpen());
		assertFalse(issue.isClosed());
		assertEquals("Preview Test", issue.title());
		assertTrue(issue.isPullRequest());

		GitHubIssuePullRequest pr = issue.pullRequest();
		assertNotNull(pr);
		assertNull(pr.mergedInstant());
	}
}
