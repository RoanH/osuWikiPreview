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
package dev.roanh.wiki.auth;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.io.netty.http.HttpParams;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.infinity.util.Scheduler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.osuapi.exception.InsufficientPermissionsException;
import dev.roanh.osuapi.exception.RequestException;
import dev.roanh.osuapi.session.OAuthSessionBuilder;
import dev.roanh.osuapi.user.Group;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.Config;
import dev.roanh.wiki.Main;

public class LoginServer{
	private final Config config;
	private final WebServer server;
	private final OAuthSessionBuilder sessionBuilder;
	private final Set<String> loginSessions = ConcurrentHashMap.newKeySet();
	//TODO metrics
	
	public LoginServer(Config config) throws IOException{
		this.config = config;
		sessionBuilder = config.getIdentitySessionBuilder();
		
		server = new WebServer(config.getLoginServerPort());
		server.setExceptionHandler(t->Main.client.logError(t, "[LoginServer] Unhandled exception", Severity.MAJOR, Priority.HIGH));
		server.createContext("/login", true, (request, path, data)->handleLoginRequest());
		server.createContext("/", true, (request, path, data)->handleLoginAttempt(request, data));
		
		try(InputStream in = ClassLoader.getSystemResourceAsStream("css/style.css")){
			server.createContext("/style.css", true, RequestHandler.sendPage(in));
		}
		
		try(InputStream in = ClassLoader.getSystemResourceAsStream("img/background.png")){
			server.createContext("/img/background.png", true, RequestHandler.sendPage(in));
		}
		
		try(InputStream in = ClassLoader.getSystemResourceAsStream("img/icon32.png")){
			server.createContext("/img/icon32.png", true, RequestHandler.sendPage(in));
		}
	}
	
	public void start(){
		server.runAsync();
	}
	
	public static void main(String[] args) throws InterruptedException, IOException{
		new LoginServer(Main.config).start();
		Thread.sleep(Duration.ofHours(40));
	}
	
	/**
	 * handles a request to login and redirects to the osu! authorisation page.
	 * @return The HTTP response.
	 */
	private final FullHttpResponse handleLoginRequest(){
		String state = UUID.randomUUID().toString();
		loginSessions.add(state);
		Scheduler.scheduleIn(15, TimeUnit.MINUTES, ()->loginSessions.remove(state));
		
		FullHttpResponse resp = RequestHandler.status(HttpResponseStatus.FOUND);
		resp.headers().add("Location", sessionBuilder.getAuthUrl(state));
		return resp;
	}
	
	/**
	 * Handles a visit to the root page potentially with login information.
	 * @param request The login attempt request (or just a root page visit).
	 * @param data The request data.
	 * @return The response page.
	 * @throws DBException When a database exception occurs.
	 * @throws RequestException When an osu! API exception occurs.
	 */
	private final FullHttpResponse handleLoginAttempt(FullHttpRequest request, HttpParams data) throws DBException, RequestException{

		//TODO handle state + token if present else generic page
		
		//keep state codes + redirect until consumed
		//need to schedule cleanup or something
		
		
		//TODO validate
		//if failure / timeout say try again
		//else
		//redirect to original page
		
		String state = data.getFirst("state");
		String code = data.getFirst("code");
		if(state == null || code == null){
			return RequestHandler.page(Pages.getRootPage(SessionManager.getUserFromSession(request)));
		}else{
			if(!loginSessions.contains(state)){
				return RequestHandler.page(Pages.getLoginTimeoutPage());
			}
			
			//TODO get user with code and return a cookie with redirect if ok
			
			//---
			
			//diff thread
			UserExtended user = null;
			try{
				user = sessionBuilder.build(code).getCurrentUser();
				
				
				
				
				
				
			}catch(InsufficientPermissionsException e){
				//user changed the requested scopes
				return RequestHandler.badRequest();
			}
			//TODO check result
			
			//---
			
			FullHttpResponse resp = RequestHandler.page(user.getUsername() + ": " + user.getUserGroups().stream().map(Group::getShortName).toList());
			try{
				SessionManager.updateUserSession(user, resp);
			}catch(DBException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return resp;
		}
	}
}
