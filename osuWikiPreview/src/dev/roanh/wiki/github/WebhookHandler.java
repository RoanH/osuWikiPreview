/*
 * osu! wiki preview site
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev) and contributors.
 * GitHub Repository: https://github.com/RoanH/osuWikiPreview
 * GitLab Repository: https://git.roanh.dev/roan/osuwikipreview
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.roanh.wiki.github;

import java.security.Key;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.JsonObject;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.io.netty.http.HttpBody;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.BodyHandler;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.github.handler.IssueCommentHandler;
import dev.roanh.wiki.github.handler.PullRequestCommitHandler;
import dev.roanh.wiki.github.handler.PullRequestOpenedHandler;
import dev.roanh.wiki.github.hooks.IssueCommentCreatedData;
import dev.roanh.wiki.github.hooks.PullRequestOpenData;
import dev.roanh.wiki.github.hooks.PullRequestSyncData;

/**
 * Small web server to handle GitHub webhook events.
 * @author Roan
 */
public class WebhookHandler implements BodyHandler{
	/**
	 * The actual web server receiving webhook events.
	 */
	private final WebServer server;
	/**
	 * The key used to validate received payloads.
	 */
	private final Key secret;
	/**
	 * A list of subscribed comment event handlers.
	 */
	private final List<IssueCommentHandler> commentHandlers = new CopyOnWriteArrayList<IssueCommentHandler>();
	/**
	 * A list of subscribed pull request creation handlers.
	 */
	private final List<PullRequestOpenedHandler> pullRequestCreateHandlers = new CopyOnWriteArrayList<PullRequestOpenedHandler>();
	/**
	 * A list of subscribed pull request commit handlers.
	 */
	private final List<PullRequestCommitHandler> pullRequestCommitHandlers = new CopyOnWriteArrayList<PullRequestCommitHandler>();
	
	/**
	 * Constructs but does not yet start a webhook handler.
	 * @param secret The secret configured to validate payloads.
	 * @param port The port to run the server on.
	 */
	public WebhookHandler(String secret, int port){
		this.server = new WebServer(port);
		this.secret = GitHub.createSigningKey(secret);
		server.setExceptionHandler(t->Main.client.logError(t, "[WebhookHandler] Unhandled exception: " + t.getMessage(), Severity.MAJOR, Priority.HIGH));
		server.createContext("/", false, (request, path, data)->RequestHandler.status(HttpResponseStatus.FORBIDDEN));
		server.createContext(HttpMethod.POST, "/", this);
	}
	
	/**
	 * Starts the webhook handler server.
	 */
	public void start(){
		server.runAsync();
	}
	
	/**
	 * Stops the webhook handler server.
	 */
	public void stop(){
		server.shutdown();
	}
	
	/**
	 * Registers a new pull issue comment handler to this webhook handler.
	 * @param handler The handler to register.
	 */
	public void addIssueCommentHandler(IssueCommentHandler handler){
		commentHandlers.add(handler);
	}
	
	/**
	 * Registers a new pull request created handler to this webhook handler.
	 * @param handler The handler to register.
	 */
	public void addPullRequestCreatedHandler(PullRequestOpenedHandler handler){
		pullRequestCreateHandlers.add(handler);
	}

	/**
	 * Registers a new pull request commit handler to this webhook handler.
	 * @param handler The handler to register.
	 */
	public void addPullRequestCommitHandler(PullRequestCommitHandler handler){
		pullRequestCommitHandlers.add(handler);
	}

	@Override
	public FullHttpResponse handle(FullHttpRequest request, HttpBody data) throws Exception{
		String payload = data.string();
		if(!validateSignature(payload, request.headers())){
			return RequestHandler.status(HttpResponseStatus.FORBIDDEN);
		}
		
		JsonObject requestObject = GitHub.getGson().fromJson(payload, JsonObject.class);
		String type = request.headers().get("X-GitHub-Event");
		switch(type){
		case "issue_comment"://pr's are also issues
			handleIssueCommentEvent(requestObject);
			break;
		case "pull_request":
			handlePullRequestEvent(requestObject);
			break;
		}
		
		return RequestHandler.ok();
	}
	
	/**
	 * Handles a received GitHub pull request event.
	 * @param json The received event payload.
	 */
	private void handlePullRequestEvent(JsonObject json){
		JsonObject obj = GitHub.getGson().fromJson(json, JsonObject.class);
		switch(obj.get("action").getAsString()){
		case "opened":
			{
				PullRequestOpenData data = GitHub.getGson().fromJson(json, PullRequestOpenData.class);
				for(PullRequestOpenedHandler handler : pullRequestCreateHandlers){
					handler.handlePullRequestOpen(data);
				}
			}
			break;
		case "synchronize":
			{
				PullRequestSyncData data = GitHub.getGson().fromJson(json, PullRequestSyncData.class);
				for(PullRequestCommitHandler handler : pullRequestCommitHandlers){
					handler.handlePullRequestCommit(data);
				}
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * Handles a received GitHub issue comment event.
	 * @param json The received event payload.
	 */
	private void handleIssueCommentEvent(JsonObject json){
		if(json.get("action").getAsString().equals("created")){
			IssueCommentCreatedData data = GitHub.getGson().fromJson(json, IssueCommentCreatedData.class);
			for(IssueCommentHandler handler : commentHandlers){
				handler.handleComment(data);
			}
		}
	}
	
	/**
	 * Validates that the given payload was signed by GitHub with the configured secret token.
	 * @param payload The payload to validate the signature of.
	 * @param headers The HTTP request headers.
	 * @return True if the signature on the given payload was valid.
	 */
	private final boolean validateSignature(String payload, HttpHeaders headers){
		String signatureHeader = headers.get("X-Hub-Signature-256");
		return signatureHeader != null && signatureHeader.length() == 64 + 7 && signatureHeader.startsWith("sha256=") && GitHub.validateSignature(secret, signatureHeader.substring(7), payload);
	}
}