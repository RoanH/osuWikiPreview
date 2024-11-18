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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

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
import dev.roanh.wiki.github.hooks.IssueCommentData;

public class WebHookHandler implements BodyHandler{
	private static final Gson gson = new Gson();//TODO configure
	private final Key secret;
	private final List<PullRequestCommentHandler> commentHandler = new ArrayList<PullRequestCommentHandler>();
	
	public WebHookHandler(String secret){
		this.secret = GitHub.createSigningKey(secret);
	}
	
	
	
	public static void init(){
		WebServer server = new WebServer(23333);
		//TODO register exception handler
		
		server.createContext("/", false, (request, data)->RequestHandler.status(HttpResponseStatus.FORBIDDEN));
		server.createContext(HttpMethod.POST, "/", new WebHookHandler("snip"));
		
		
		server.run();
	}
	
	
	public static void main(String[] args){
		init();
	}
	
	public void addPullRequestCommentHandler(PullRequestCommentHandler handler){
		commentHandler.add(handler);
	}

	@Override
	public FullHttpResponse handle(FullHttpRequest request, HttpBody data) throws Exception{
		String payload = data.string();
		if(!validateSignature(payload, request.headers())){
			System.out.println("payload validation failed: " + payload);
			return RequestHandler.status(HttpResponseStatus.FORBIDDEN);
		}
		
		
		System.out.println("received: " + payload);

		//check if known contributor?
		
		//TODO not receiving comments right now
		
		//separate table for pr (id/number) - comment
		
		
//		new Gson().fromJson(signatureHeader, null)
		
		
		// TODO Auto-generated method stub
		
		
		String type = request.headers().get("X-GitHub-Event");
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
			for(PullRequestCommentHandler handler : commentHandler){
				handler.handleComment(data);
			}
		}
	}
	
	
	
	
	
	
	
	
	
	

	
	private final boolean validateSignature(String payload, HttpHeaders headers){
		String signatureHeader = headers.get("X-Hub-Signature-256");
		return signatureHeader != null && signatureHeader.length() == 64 + 7 && signatureHeader.startsWith("sha256=") && GitHub.validateSignature(secret, signatureHeader.substring(7), payload);
	}
}
