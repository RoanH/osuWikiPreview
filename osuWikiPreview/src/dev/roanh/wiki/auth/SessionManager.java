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

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.util.Base64;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.MainDatabase;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.auth.LoginServer.LoginInfo;
import dev.roanh.wiki.data.User;

/**
 * User session and cookie manager.
 * @author Roan
 */
public final class SessionManager{
	/**
	 * Name of the wiki preview session header cookie.
	 */
	private static final String SESSION_HEADER = "wiki_preview_session";
	/**
	 * Secure random generator for user session tokens.
	 */
	private static final SecureRandom random = new SecureRandom();
	
	/**
	 * Prevent instantiation.
	 */
	private SessionManager(){
	}
	
	/**
	 * Gets the user associated with the given incoming request.
	 * @param request The incoming web request.
	 * @return The user for the given request if any, else null.
	 * @throws DBException When a database exception occurs.
	 */
	protected static User getUserFromSession(FullHttpRequest request) throws DBException{
		String header = request.headers().get(HttpHeaderNames.COOKIE);
		if(header != null){
			for(Cookie cookie : ServerCookieDecoder.STRICT.decode(header)){
				if(cookie.name().equals(SESSION_HEADER)){
					return getUserFromSession(cookie);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the user associated with the given session cookie.
	 * @param sessionCookie The session cookie.
	 * @return The user for the given cookie if any, else null.
	 * @throws DBException When a database exception occurs.
	 */
	protected static User getUserFromSession(Cookie sessionCookie) throws DBException{
		return MainDatabase.getUserBySession(sessionCookie.value());
	}
	
	/**
	 * Updates the session token for the given user.
	 * @param user The user to update the session for.
	 * @param info Metadata associated with the login.
	 * @return The updated user session cookie.
	 * @throws DBException When a database exception occurs.
	 */
	protected static Cookie updateUserSession(UserExtended user, LoginInfo info) throws DBException{
		String session = generateToken();
		MainDatabase.saveUserSession(user, session, info);
		
		if(info.discordId().isPresent()){
			syncDiscord(MainDatabase.getUserBySession(session));
		}
		
		Cookie cookie = new DefaultCookie(SESSION_HEADER, session);
		cookie.setDomain(Main.config.domain());
		cookie.setMaxAge(TimeUnit.DAYS.toSeconds(365));
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		return cookie;
	}
	
	/**
	 * Synchronises private mode instance access for the given user.
	 * @param user The user to synchronise access for.
	 */
	protected static void syncDiscord(User user){
		for(OsuWeb web : InstanceManager.getInstances()){
			if(web.getInstance().isPrivateMode()){
				web.getAccessManager().syncAccess(user);
			}
		}
	}
	
	/**
	 * Generates a new random token.
	 * @return The generated token.
	 */
	private static final String generateToken(){
		byte[] data = new byte[512];
		random.nextBytes(data);
		return Base64.encode(data).string();
	}
}
