package dev.roanh.wiki.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import dev.roanh.infinity.util.Variable;
import dev.roanh.wiki.github.hooks.IssueCommentData;
import dev.roanh.wiki.github.obj.GitHubComment;
import dev.roanh.wiki.github.obj.GitHubIssue;
import dev.roanh.wiki.github.obj.GitHubIssuePullRequest;
import dev.roanh.wiki.github.obj.GitHubUser;

public class WebHookHandlerTest{
	private static final Key DEFAULT_KEY = GitHub.createSigningKey("Test");
	private static final HttpClient client = HttpClient.newHttpClient();
	
	@Test
	public void commentEvent() throws Exception{
		WebHookHandler handler = new WebHookHandler("Test");
		handler.start();
		
		try{
			Variable<IssueCommentData> var = new Variable<IssueCommentData>();
			CountDownLatch latch = new CountDownLatch(1);
			
			handler.addPullRequestCommentHandler(data->{
				var.setValue(data);
				latch.countDown();
			});
			
			sendJson("pr_comment_created", "issue_comment", DEFAULT_KEY);
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
		}finally{
			handler.stop();
		}
	}
	
	private static void sendJson(String name, String eventType, Key signingKey) throws Exception{
		try(InputStream in = ClassLoader.getSystemResourceAsStream("resources/github/" + name + ".json")){
			String payload = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			HttpRequest.Builder builder = HttpRequest.newBuilder(new URI("http://localhost:23333"));
			builder.header("X-Hub-Signature-256", "sha256=" + HexFormat.of().formatHex(GitHub.sign(signingKey, payload)));
			builder.header("X-GitHub-Event", eventType);
			builder.POST(BodyPublishers.ofString(payload));
			
			assertEquals(200, client.send(builder.build(), BodyHandlers.discarding()).statusCode());
		}
	}
}
