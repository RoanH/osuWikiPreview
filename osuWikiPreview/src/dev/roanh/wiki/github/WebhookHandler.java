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

import java.io.IOException;
import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.io.netty.http.HttpBody;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.BodyHandler;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.wiki.github.handler.PullRequestCommentHandler;
import dev.roanh.wiki.github.handler.PullRequestCommitHandler;
import dev.roanh.wiki.github.handler.PullRequestCreatedHandler;
import dev.roanh.wiki.github.hooks.IssueCommentData;

public class WebhookHandler implements BodyHandler{
	private static final Gson gson;
	private final WebServer server;
	private final Key secret;
	private final List<PullRequestCommentHandler> commentHandlers = new ArrayList<PullRequestCommentHandler>();
	private final List<PullRequestCreatedHandler> createHandlers = new ArrayList<PullRequestCreatedHandler>();
	private final List<PullRequestCommitHandler> commitHandlers = new ArrayList<PullRequestCommitHandler>();
	
	public WebhookHandler(String secret){
		this.server = new WebServer(23333);
		this.secret = GitHub.createSigningKey(secret);
		//TODO register exception handler
		server.createContext("/", false, (request, data)->RequestHandler.status(HttpResponseStatus.FORBIDDEN));
		server.createContext(HttpMethod.POST, "/", this);
	}
	
	
	
	
	
	
	public void start(){
		server.runAsync();
	}
	
	public void stop(){
		server.shutdown();
	}
	
	public void addPullRequestCommentHandler(PullRequestCommentHandler handler){
		commentHandlers.add(handler);
	}
	
	public void addPullRequestCreatedHandler(PullRequestCreatedHandler handler){
		createHandlers.add(handler);
	}

	public void addPullRequestCommitHandler(PullRequestCommitHandler handler){
		commitHandlers.add(handler);
	}

	@Override
	public FullHttpResponse handle(FullHttpRequest request, HttpBody data) throws Exception{
		String payload = data.string();
		if(!validateSignature(payload, request.headers())){
			System.out.println("payload validation failed: " + "\nsig: " + request.headers().get("X-Hub-Signature-256"));
			return RequestHandler.status(HttpResponseStatus.FORBIDDEN);
		}
		
		
		System.out.println("received: " + payload);

		//check if known contributor?
		
		//TODO not receiving comments right now
		
		//separate table for pr (id/number) - comment
		
		
//		new Gson().fromJson(signatureHeader, null)
		
		
		// TODO Auto-generated method stub
		
		
		String type = request.headers().get("X-GitHub-Event");
		System.out.println("type: " + type);
		switch(type){
		case "issue_comment"://pr's are also issues
			handleIssueCommentEvent(payload);
			break;
		}
		
		return RequestHandler.ok();
	}
	
	private void handleIssueCommentEvent(String json) throws IOException{
		IssueCommentData data = gson.fromJson(json, IssueCommentData.class);
		if(data.isCreateAction() && data.issue().isPullRequest()){
			for(PullRequestCommentHandler handler : commentHandlers){
				handler.handleComment(data);
			}
		}
	}
	
	
	
	
	
	
	
	
	
	

	
	private final boolean validateSignature(String payload, HttpHeaders headers){
		String signatureHeader = headers.get("X-Hub-Signature-256");
		return signatureHeader != null && signatureHeader.length() == 64 + 7 && signatureHeader.startsWith("sha256=") && GitHub.validateSignature(secret, signatureHeader.substring(7), payload);
	}
	
	
	
	static{
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Instant.class, new InstantDeserializer());
		gson = builder.create();
	}
}