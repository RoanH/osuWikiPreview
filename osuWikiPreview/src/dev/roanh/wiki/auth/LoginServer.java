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
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.prometheus.client.Counter;

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
import dev.roanh.wiki.Config;
import dev.roanh.wiki.Main;

public class LoginServer{
	/**
	 * Number of login session started.
	 */
	private static final Counter loginStarted = Counter.build("wikipreview_login_started", "Number of login URLs created").register();
	/**
	 * Result of login attempts.
	 */
	private static final Counter loginAttempts = Counter.build("wikipreview_login_result", "Login activity by result").labelNames("result").register();
	/**
	 * The server used to handle web requests.
	 */
	private final WebServer server;
	/**
	 * Builder for OAuth sessions.
	 */
	private final OAuthSessionBuilder sessionBuilder;
	/**
	 * Map of active login sessions.
	 */
	private final Map<String, LoginInfo> loginSessions = new ConcurrentHashMap<String, LoginInfo>();
	
	/**
	 * Constructs a new login server with the given config.
	 * @param config The server configuration.
	 * @throws IOException When an IOException occurs creating the server.
	 */
	public LoginServer(Config config) throws IOException{
		sessionBuilder = config.getIdentitySessionBuilder();
		
		server = new WebServer(config.getLoginServerPort());
		server.setExceptionHandler(t->Main.client.logError(t, "[LoginServer] Unhandled exception", Severity.MAJOR, Priority.HIGH));
		server.createContext("/login", true, this::handleLoginRequest);
		server.createContext("/", true, this::handleLoginAttempt);
		
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
	
	/**
	 * Starts this server.
	 */
	public void start(){
		server.runAsync();
	}
	
	/**
	 * Creates a new authentication URL with the given metadata.
	 * @param info The login metadata.
	 * @return The created osu! OAuth authentication URL.
	 */
	public String createAuthUrl(LoginInfo info){
		String state = UUID.randomUUID().toString();
		loginSessions.put(state, info);
		Scheduler.scheduleIn(15, TimeUnit.MINUTES, ()->loginSessions.remove(state));
		loginStarted.inc();
		return sessionBuilder.getAuthUrl(state);
	}
	
	/**
	 * handles a request to login and redirects to the osu! authorisation page.
	 * @param request The incoming HTTP request.
	 * @param path The request path (always /login).
	 * @param data The request data.
	 * @return The HTTP response.
	 */
	private final FullHttpResponse handleLoginRequest(FullHttpRequest request, String path, HttpParams data){
		FullHttpResponse resp = RequestHandler.status(HttpResponseStatus.FOUND);
		resp.headers().add("Location", createAuthUrl(new LoginInfo()));
		return resp;
	}
	
	/**
	 * Handles a visit to the root page potentially with login information.
	 * @param request The login attempt request (or just a root page visit).
	 * @param path The request path (always the root /).
	 * @param data The request data.
	 * @return The response page.
	 * @throws DBException When a database exception occurs.
	 * @throws RequestException When an osu! API exception occurs.
	 */
	private final FullHttpResponse handleLoginAttempt(FullHttpRequest request, String path, HttpParams data) throws DBException, RequestException{
		String state = data.getFirst("state");
		String code = data.getFirst("code");
		if(state == null || code == null){
			return RequestHandler.page(Pages.getRootPage(SessionManager.getUserFromSession(request)));
		}else{
			LoginInfo info = loginSessions.get(state);
			if(info == null){
				loginAttempts.labels("timeout").inc();
				return RequestHandler.page(Pages.getLoginTimeoutPage());
			}
			
			try{
				Cookie session = SessionManager.updateUserSession(sessionBuilder.build(code).getCurrentUser(), info);
				FullHttpResponse resp = RequestHandler.page(Pages.getRootPage(SessionManager.getUserFromSession(session)));
				resp.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(session));
				loginAttempts.labels("success").inc();
				return resp;
			}catch(InsufficientPermissionsException e){
				//user changed the requested scopes
				loginAttempts.labels("invalid").inc();
				return RequestHandler.badRequest();
			}
		}
	}
	
	/**
	 * Record with metadata about a login session.
	 * @author Roan
	 * @param discordId The discord ID of the user that initiated the login.
	 */
	public static record LoginInfo(OptionalLong discordId){
		
		/**
		 * Constructs new empty login metadata.
		 */
		public LoginInfo(){
			this(OptionalLong.empty());
		}
		
		/**
		 * Constructs login metadata for the given Discord user.
		 * @param discordId The ID of the Discord user.
		 */
		public LoginInfo(long discordId){
			this(OptionalLong.of(discordId));
		}
	}
}
