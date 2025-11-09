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
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.data.User;

/**
 * Set of HTTP pages shown to the user on the root domain and error pages.
 * @author Roan
 */
public final class Pages{
	/**
	 * Template HTML page with the general structure.
	 */
	private static final String TEMPLATE;

	/**
	 * Prevent instantiation.
	 */
	private Pages(){
	}
	
	/**
	 * Gets the root page shown when the root of the domain is visited. This
	 * page just lists the instances and some account information if present.
	 * @param user The logged in user if any, else null.
	 * @return The root page HTML.
	 */
	public static final String getRootPage(User user){
		StringBuilder buffer = new StringBuilder();
		buffer.append("Welcome to the osu! <a href=\"https://osu.ppy.sh/wiki/en/Main_page\">wiki</a> and <a href=\"https://osu.ppy.sh/home/news\">news</a> preview site.");
		buffer.append("The available preview instances can be found at the following subdomains:");
		
		buffer.append("<ul>");
		for(OsuWeb web : InstanceManager.getInstances()){
			buffer.append("<li><a href=\"");
			buffer.append(web.getInstance().getSiteUrl());
			buffer.append("\">");
			buffer.append(web.getInstance().getDomain());
			buffer.append("</a></li>");
		}
		buffer.append("</ul>");
		
		if(user == null){
			buffer.append("If you have been invited to view private preview instances you need to log in below.");
			buffer.append("<br><br>");
			buffer.append("<a class=\"button\" href=\"https://preview.roanh.dev/login\">Login</a>");
		}else{
			buffer.append("You are currently logged in as <a href=\"https://osu.ppy.sh/users/");
			buffer.append(user.osuId());
			buffer.append("\">");
			buffer.append(user.osuName());
			buffer.append("</a>.");
		}
		
		return makePage("osu! wiki preview", "osu! wiki preview", buffer.toString());
	}
	
	/**
	 * Gets the page when a user without access tries to access a private mode instance.
	 * This page just has some general information and a login button.
	 * @param user The logged in user if any, else null.
	 * @return The created private mode page HTML.
	 */
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
	
	/**
	 * Gets the page shown when a user wait too long when logging in.
	 * @return The login timeout page HTML.
	 */
	public static final String getLoginTimeoutPage(){
		return makePage(
			"Login Timeout",
			"""
			Your login session timed out, please try again.
			<br><br>
			<a class="button" href="https://preview.roanh.dev/login">Login</a>
			"""
		);
	}
	
	/**
	 * Creates a page with the given content and title.
	 * @param title The page title and header.
	 * @param content The HTML page content.
	 * @return The page HTML source.
	 */
	private static final String makePage(String title, String content){
		return makePage(title + " | osu! wiki preview", title, content);
	}
		
	/**
	 * Creates a page with the given content.
	 * @param title The page title.
	 * @param header The page header.
	 * @param content The page HTML content.
	 * @return The page HTML source.
	 */
	private static final String makePage(String title, String header, String content){
		return TEMPLATE.replace("HEADER", header).replace("TITLE", title).replace("CONTENT", content);
	}
	
	static{
		String version = Optional.ofNullable(Util.returnOrNull(Version::readVersion)).map(a->"v" + a.getVersion()).orElse("unknown");
		try(InputStream in = ClassLoader.getSystemResourceAsStream("html/index.html")){
			TEMPLATE = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("VERSION", version);
		}catch(IOException ignore){
			IllegalStateException cause = new IllegalStateException("Failed to load web resources.", ignore);
			Main.client.logError(cause, "[Pages] Failed to load web resources", Severity.MAJOR, Priority.HIGH);
			throw cause;
		}
	}
}
