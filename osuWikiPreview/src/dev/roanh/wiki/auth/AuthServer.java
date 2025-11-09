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

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.prometheus.client.Counter;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.io.netty.http.HttpParams;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.User;

/**
 * NGINX authentication server.
 * @author Roan
 * @see LoginServer
 */
public class AuthServer{
	/**
	 * Metric for authentication request statuses.
	 */
	private static final Counter authRequests = Counter.build("wikipreview_auth_request", "Number auth requests by result").labelNames("result").register();
	/**
	 * The header used to pass the instance that was attempted to be accessed.
	 */
	private static final String INSTANCE_HEADER = "Instance-Domain";
	/**
	 * The HTTP server used to communicate with NGINX.
	 */
	private final WebServer server;
	
	/**
	 * Creates a new authentication server running on the given port.
	 * @param port The port the server should run on.
	 */
	public AuthServer(int port){
		server = new WebServer(port);
		server.setExceptionHandler(t->Main.client.logError(t, "[AuthServer] Unhandled exception", Severity.MAJOR, Priority.HIGH));
		server.createContext("/login", true, this::handleLoginErrorPage);
		server.createContext("/auth", true, this::handleAuthRequest);
	}
	
	/**
	 * Starts the authentication server.
	 */
	public void start(){
		server.runAsync();
	}
	
	/**
	 * Handles the error page request generated when a user does not have access to an instance.
	 * <p>
	 * This gives the user some basic information and tells them to login or request access.
	 * @param request The error page request.
	 * @param path The path, always /login.
	 * @param data The request data.
	 * @return The response page.
	 * @throws DBException When a database exception occurs.
	 * @see #handleAuthRequest(FullHttpRequest, String, HttpParams)
	 */
	private FullHttpResponse handleLoginErrorPage(FullHttpRequest request, String path, HttpParams data) throws DBException{
		return RequestHandler.page(Pages.getPrivateModePage(SessionManager.getUserFromSession(request)));
	}
	
	/**
	 * Handles a request from NGINX to check if a user has access to a specific instance.
	 * @param request The authentication request.
	 * @param path The request path, always /auth.
	 * @param data The request data.
	 * @return The response page, either OK if the user has access or UNAUTHORIZED if they do not.
	 * @throws DBException When a database exception occurs.
	 * @see #handleLoginErrorPage(FullHttpRequest, String, HttpParams)
	 */
	private FullHttpResponse handleAuthRequest(FullHttpRequest request, String path, HttpParams data) throws DBException{
		Instance instance = InstanceManager.getInstanceByDomain(request.headers().get(INSTANCE_HEADER)).getInstance();
		if(!instance.isPrivateMode()){
			authRequests.labels("public").inc();
			return RequestHandler.ok();
		}
		
		User user = SessionManager.getUserFromSession(request);
		if(user == null){
			authRequests.labels("private_not_logged_in").inc();
			return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);
		}
		
		if(instance.getAccessList().contains(user)){
			authRequests.labels("private_on_acl").inc();
			return RequestHandler.ok();
		}else{
			authRequests.labels("private_not_on_acl").inc();
			return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);
		}
	}
}
