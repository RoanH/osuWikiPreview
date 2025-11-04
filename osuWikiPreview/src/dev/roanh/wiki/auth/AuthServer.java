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
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.io.netty.http.HttpParams;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.MainDatabase;
import dev.roanh.wiki.data.User;

public class AuthServer{
	private static final String INSTANCE_HEADER = "Instance-Domain";
	
	
	//TODO metrics
	
	
	//401 on auth fail with redirect
	//200 on OK
	
	//TODO probably need a discord link too?
	
	
	
	
	//TODO remove
	private static void printAll(FullHttpRequest request, String path, HttpParams data){
		System.out.println("p: " + path);
		
		System.out.println(data.getKeys());
		for(String key : data.getKeys()){
			System.out.println(key + " : " + data.get(key));
		}
		
		request.headers().forEach(e->{
			System.out.println(e.getKey() + " : " + e.getValue());
		});
	}
	
	public static void main(String[] args) throws InterruptedException, IOException, DBException{
		//TODO logging & error handler config for servers
		
		MainDatabase.init(Main.config);
		InstanceManager.init(Main.config);
		
		WebServer server = new WebServer(1234);
		
		server.createContext("/login", true, (request, path, data)->{
			return RequestHandler.page(Pages.getPrivateModePage(SessionManager.getUserFromSession(request)));
		});
		
		server.createContext("/auth", true, (request, path, data)->{
			printAll(request, path, data);//TODO remove
//			Instance instance = InstanceManager.getInstanceByDomain(request.headers().get(INSTANCE_HEADER)).getInstance();
//			if(!instance.isPrivateMode()){
//				return RequestHandler.ok();
//			}
			
			User user = SessionManager.getUserFromSession(request);
			if(user == null){
				return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);
			}
			
			//TODO check if user has access or not, now everyone has access
			return RequestHandler.status(HttpResponseStatus.OK);
		});
		
		
		//include either the oauth uri in the page or redirect idk yet -- link click would mean longer timeout, otherwise its from pagel oad
		
		server.runAsync();
		
		
		//---
		
		LoginServer.main(null);
		
		
	}
}
