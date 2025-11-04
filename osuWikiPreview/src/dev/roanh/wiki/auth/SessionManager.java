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
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.infinity.util.Base64;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.MainDatabase;
import dev.roanh.wiki.data.User;

public class SessionManager{
	private static final String SESSION_HEADER = "wiki_preview_session";
	/**
	 * Secure random generator for user session tokens.
	 */
	private static final SecureRandom random = new SecureRandom();
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	protected static User getUserFromSession(FullHttpRequest request) throws DBException{
		String header = request.headers().get("Cookie");
		if(header != null){
			for(Cookie cookie : ServerCookieDecoder.STRICT.decode(header)){
				if(cookie.name().equals(SESSION_HEADER)){
					return MainDatabase.getUserBySession(cookie.value());
				}
			}
		}
		
		return null;
	}
	
	protected static void updateUserSession(UserExtended user, FullHttpResponse response) throws DBException{
		String session = generateToken();
		MainDatabase.saveUserSession(user.getId(), session);
		
		Cookie cookie = new DefaultCookie(SESSION_HEADER, session);
		cookie.setDomain(Main.config.domain());
		cookie.setMaxAge(TimeUnit.DAYS.toSeconds(365));
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		response.headers().add("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie));
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
