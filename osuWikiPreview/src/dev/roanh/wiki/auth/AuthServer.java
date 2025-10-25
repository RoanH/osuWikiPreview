package dev.roanh.wiki.auth;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.io.netty.http.HttpParams;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;
import dev.roanh.osuapi.OsuAPI;
import dev.roanh.osuapi.Scope;
import dev.roanh.osuapi.session.OAuthSessionBuilder;
import dev.roanh.osuapi.session.RefreshTokenStore;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.Main;

public class AuthServer{

	//TODO metrics
	
	//osu6.preview.roanh.dev as test I guess
	
	//401 on auth fail with redirect
	//200 on OK
	
	//TODO probably need a discord link too?
	
	
	
	
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
			return RequestHandler.status(HttpResponseStatus.UNAUTHORIZED);//200 OK, 401 UNAUTH
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
			

			OAuthSessionBuilder builder = OsuAPI.oauth(Main.CLIENT_ID, null);
			builder.setCallback("https://preview.roanh.dev/");
			builder.addScopes(Scope.IDENTIFY);
			
			//diff thread
//			builder.build().getCurrentUser();
			
			//todo return redirect to auth url
			FullHttpResponse resp = RequestHandler.status(HttpResponseStatus.FOUND);
				resp.headers().add("Location", builder.getAuthUrl());
				return resp;
		});
		
		Map<UUID, String> authSessions = new ConcurrentHashMap<UUID, String>();
		
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
				return RequestHandler.page("Hi, there's nothing here really, you probably want to go to one of the preview subdomains:");//TODO extract and stuff
			}else{
				String origin = authSessions.get(state);
				if(origin == null){
					return RequestHandler.page("Your login session timed out, please try again: ");//TODO add link idk
				}
				
				//TODO get user with code and return a cookie with redirect if ok
				
				
				
				
				//---
				
				OAuthSessionBuilder builder = OsuAPI.oauth(Main.CLIENT_ID, Main.CLIENT_SECRET);
				
//				builder.getAuthUrl();
				
//				builder.setAuthPrompt(auth->{});
//				builder.setCallback("https://preview.roanh.dev/");
				builder.setRefreshStore(new RefreshTokenStore(){

					@Override
					public String readRefreshToken() throws IOException{
						return code;
					}

					@Override
					public void writeRefreshToken(String token) throws IOException{
					}
				});
//				builder.setTokenProvider(new TokenProvider(){
//
//					@Override
//					public String getCode(String state) throws IOException, TimeoutException{
//						// TODO Auto-generated method stub
//						return null;
//					}
//				});
				builder.addScopes(Scope.IDENTIFY);
				
				//diff thread
				UserExtended user = builder.build().getCurrentUser();
				//TODO check result
				
				//---
				
				
				
				System.out.println(user.getUsername());
				
				
				
				return RequestHandler.page(user.getUsername() + ": " + user.getUserGroups());
				
				
				
				
			}
		});

		authServer.run();
	}
}
