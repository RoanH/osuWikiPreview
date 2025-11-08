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

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.io.netty.http.HttpParams;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.MainDatabase;
import dev.roanh.wiki.data.AccessList;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.User;

public class AuthServer{
	private static final String INSTANCE_HEADER = "Instance-Domain";
	private final WebServer server;
	//TODO metrics
	
	public AuthServer(int port){
		server = new WebServer(port);
		server.setExceptionHandler(t->Main.client.logError(t, "[AuthServer] Unhandled exception", Severity.MAJOR, Priority.HIGH));
		server.createContext("/login", true, this::handleLoginErrorPage);
		server.createContext("/auth", true, this::handleAuthRequest);
	}
	
	public void start(){
		server.runAsync();
	}
	
	private FullHttpResponse handleLoginErrorPage(FullHttpRequest request, String path, HttpParams data) throws DBException{
		return RequestHandler.page(Pages.getPrivateModePage(SessionManager.getUserFromSession(request)));
	}
	
	private FullHttpResponse handleAuthRequest(FullHttpRequest request, String path, HttpParams data) throws DBException{
		Instance instance = InstanceManager.getInstanceByDomain(request.headers().get(INSTANCE_HEADER)).getInstance();
		if(!instance.isPrivateMode()){
			return RequestHandler.ok();
		}
		
		User user = SessionManager.getUserFromSession(request);
		if(user == null){
			return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);
		}
		
		if(instance.getAccessList().contains(user)){
			return RequestHandler.ok();
		}else{
			return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);
		}
	}
	
	//TODO probably need a discord link too?
	
	public static void main(String[] args) throws InterruptedException, IOException, DBException{
		MainDatabase.init(Main.config);
		InstanceManager.init(Main.config);
		
		AccessList acl = new AccessList();
		Instance i = InstanceManager.getInstanceByDomain("osu5.preview.roanh.dev").getInstance();
		i.setAccessList(acl);
		MainDatabase.saveInstance(i);
		
		new AuthServer(1234).start();
		new LoginServer(Main.config).start();
	}
}
