package dev.roanh.wiki.auth;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.io.netty.http.HttpParams;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.infinity.util.Scheduler;
import dev.roanh.osuapi.OsuAPI;
import dev.roanh.osuapi.Scope;
import dev.roanh.osuapi.session.OAuthSessionBuilder;
import dev.roanh.osuapi.user.Group;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.Main;

public class AuthServer{
	private static final OAuthSessionBuilder sessionBuilder = OsuAPI.oauth(Main.CLIENT_ID, Main.CLIENT_SECRET).setCallback("https://preview.roanh.dev/").addScopes(Scope.IDENTIFY);
	
	//TODO not static?
	private static final Set<String> loginSessions = ConcurrentHashMap.newKeySet();
	
	
	//TODO metrics
	
	//osu6.preview.roanh.dev as test I guess
	
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
	
	public static void main(String[] args){
		WebServer server = new WebServer(1234);
		
		server.createContext("/", false, (request, path, data)->{
			printAll(request, path, data);
			return RequestHandler.notFound();
		});
	
		server.createContext("/login", true, (request, path, data)->{
			printAll(request, path, data);
			return RequestHandler.page("Please login!, go here: https://preview.roanh.dev/login");
		});
		
		server.createContext("/auth", true, (request, path, data)->{
			printAll(request, path, data);
			
			//TODO do better
			if(request.headers().get("Cookie").contains("wiki_preview_session=test")){
				return RequestHandler.status(HttpResponseStatus.OK);
			}else{
				return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);
			}
		});
		
		
		//include either the oauth uri in the page or redirect idk yet -- link click would mean longer timeout, otherwise its from pagel oad
		
		server.runAsync();
		
		
		//---
		
		
		WebServer authServer = new WebServer(1235);
//		authServer.createContext("/", false, (request, path, data) -> {
//			System.out.println("ap: " + path);
//
//			System.out.println(data.getKeys());
//			for(String key : data.getKeys()){
//				System.out.println(key + " a: " + data.get(key));
//			}
//
//			request.headers().forEach(e->{
//				System.out.println(e.getKey() + " a: " + e.getValue());
//			});
//
//			return RequestHandler.status(HttpResponseStatus.OK);
//		});
		
		
		authServer.createContext("/login", true, (request, path, data)->{
			printAll(request, path, data);
			
			String state = UUID.randomUUID().toString();
			loginSessions.add(state);
			Scheduler.scheduleIn(15, TimeUnit.MINUTES, ()->loginSessions.remove(state));
			
			FullHttpResponse resp = RequestHandler.status(HttpResponseStatus.FOUND);
			resp.headers().add("Location", sessionBuilder.getAuthUrl(state));
			return resp;
		});
		
		
		authServer.createContext("/", true, (request, path, data)->{
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
				return RequestHandler.page("Hi, there's nothing here really, you probably want to go to one of the preview subdomains:");//TODO extract and stuff, maybe some login info, relog for group fix idk
			}else{
				if(!loginSessions.contains(state)){
					return RequestHandler.page("Your login session timed out, please try again: ");//TODO add link idk
				}
				
				//TODO get user with code and return a cookie with redirect if ok
				
				
				
				
				//---
				
				
				
				//diff thread
				UserExtended user = sessionBuilder.build(code).getCurrentUser();
				//TODO check result
				
				//---
				
				
				
				System.out.println(user.getUsername());
				
				
				
				FullHttpResponse resp = RequestHandler.page(user.getUsername() + ": " + user.getUserGroups().stream().map(Group::getShortName).toList());
				String session = "test";//TODO 300-400 chars or so
				resp.headers().add("Set-Cookie", "wiki_preview_session=" + session + "; Secure; HttpOnly; Max-Age=31536000; Domain=preview.roanh.dev");
				return resp;
				
				
				
			}
		});

		authServer.run();
	}
}
