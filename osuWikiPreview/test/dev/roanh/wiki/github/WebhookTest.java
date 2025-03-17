package dev.roanh.wiki.github;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HexFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class WebhookTest{
	protected static final Key DEFAULT_KEY = GitHub.createSigningKey("Test");
	protected static final HttpClient client = HttpClient.newHttpClient();
	protected WebhookHandler webhook;
	
	@BeforeEach
	public void startWebhookHandler(){
		webhook = new WebhookHandler("Test");
		webhook.start();
	}
	
	@AfterEach
	public void stopWebhookHandler(){
		webhook.stop();
	}
	
	protected static void sendPullRequestCommentPayload() throws Exception{
		sendJson("pr_comment_created", "issue_comment", DEFAULT_KEY);
	}
	
	protected static void sendJson(String name, String eventType, Key signingKey) throws Exception{
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
