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
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import dev.roanh.infinity.util.Util;
import dev.roanh.infinity.util.Version;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.data.User;

public final class Pages{
	private static final String TEMPLATE;

	private Pages(){
	}
	
	public static final String getRootPage(User user){
		return makePage("Hello World", """
		    Hi, there's nothing here really, you probably want to go to one of the preview subdomains:
			<br>
			<a href="https://roanh.dev/">This is a link</a>
		    """);
		
		
		
		
		
		//nullable user
		//TODO
		//TODO extract and stuff, maybe some login info, relog for group fix idk
//		return "Hi, there's nothing here really, you probably want to go to one of the preview subdomains:";
	}
	
	public static final String getPrivateModePage(User user){
		if(user == null){
			return makePage(
				"Private Mode",
				"""
				This preview instance is currently in private mode, if you have access please log in with your osu! account to view pages.
				<br><br>
				<a class="button" href="https://preview.roanh.dev/login">Login</a>
				"""
			);
		}else{
			return makePage(
				"Private Mode",
				"""
				This preview instance is currently in private mode, you are logged in as <a href="https://osu.ppy.sh/users/%d">%s</a> but do not appear to have access.
				If you joined a user group with access after last logging in your groups might not have been updated yet, in this case you could try logging in again below.
				<br><br>
				<a class="button" href="https://preview.roanh.dev/login">Login</a>
				""".formatted(user.osuId(), user.osuName())
			);
		}
	}
	
	private static final String makePage(String title, String content){
		return TEMPLATE.replace("TITLE", title).replace("CONTENT", content);
	}
	
	static{
		String version = Optional.ofNullable(Util.returnOrNull(Version::readVersion)).map(a->"v" + a.getVersion()).orElse("unknown");
		try(InputStream in = ClassLoader.getSystemResourceAsStream("html/index.html")){
			TEMPLATE = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("VERSION", version);
		}catch(IOException e){
			IllegalStateException cause = new IllegalStateException("Failed to load web resources.", e);
			Main.client.logError(cause, "[Pages] Failed to load web resources", Severity.MAJOR, Priority.HIGH);
			throw cause;
		}
	}
}
