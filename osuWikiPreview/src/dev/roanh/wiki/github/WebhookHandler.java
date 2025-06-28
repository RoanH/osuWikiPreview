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
import java.util.ArrayList;
import java.util.List;

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
import dev.roanh.wiki.github.handler.IssueCommentHandler;
import dev.roanh.wiki.github.handler.PullRequestCommitHandler;
import dev.roanh.wiki.github.handler.PullRequestOpenedHandler;
import dev.roanh.wiki.github.hooks.IssueCommentCreatedData;
import dev.roanh.wiki.github.hooks.PullRequestOpenData;
import dev.roanh.wiki.github.hooks.PullRequestSyncData;

public class WebhookHandler implements BodyHandler{
	private final WebServer server;
	private final Key secret;
	private final List<IssueCommentHandler> commentHandlers = new ArrayList<IssueCommentHandler>();
	private final List<PullRequestOpenedHandler> pullRequestCreateHandlers = new ArrayList<PullRequestOpenedHandler>();
	private final List<PullRequestCommitHandler> pullRequestCommitHandlers = new ArrayList<PullRequestCommitHandler>();
	
	public WebhookHandler(String secret){
		this.server = new WebServer(23333);
		this.secret = GitHub.createSigningKey(secret);
		//TODO register exception handler
		server.createContext("/", false, (request, path, data)->RequestHandler.status(HttpResponseStatus.FORBIDDEN));
		server.createContext(HttpMethod.POST, "/", this);
	}
	
	public static void main(String[] args) throws InterruptedException{
		WebhookHandler handler = new WebhookHandler("");
		handler.start();
		Thread.sleep(1000000000000L);
	}
	
	public void start(){
		server.runAsync();
	}
	
	public void stop(){
		server.shutdown();
	}
	
	//TODO event for PR merges to know an instance can be freed
	
	public void addIssueCommentHandler(IssueCommentHandler handler){
		commentHandlers.add(handler);
	}
	
	public void addPullRequestCreatedHandler(PullRequestOpenedHandler handler){
		pullRequestCreateHandlers.add(handler);
	}

	public void addPullRequestCommitHandler(PullRequestCommitHandler handler){
		pullRequestCommitHandlers.add(handler);
	}

	@Override
	public FullHttpResponse handle(FullHttpRequest request, HttpBody data) throws Exception{
		String payload = data.string();
		
		System.out.println("received: " + payload);

		
		if(!validateSignature(payload, request.headers())){
			System.out.println("payload validation failed: " + "\nsig: " + request.headers().get("X-Hub-Signature-256"));
			return RequestHandler.status(HttpResponseStatus.FORBIDDEN);
		}
		
		
		
		//separate table for pr (id/number) - comment
		
		
		
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
		}
	}
	
	private void handleIssueCommentEvent(JsonObject json){
		if(json.get("action").getAsString().equals("created")){
			IssueCommentCreatedData data = GitHub.getGson().fromJson(json, IssueCommentCreatedData.class);
			for(IssueCommentHandler handler : commentHandlers){
				handler.handleComment(data);
			}
		}
	}
	
	private final boolean validateSignature(String payload, HttpHeaders headers){
		String signatureHeader = headers.get("X-Hub-Signature-256");
		return signatureHeader != null && signatureHeader.length() == 64 + 7 && signatureHeader.startsWith("sha256=") && GitHub.validateSignature(secret, signatureHeader.substring(7), payload);
	}
}