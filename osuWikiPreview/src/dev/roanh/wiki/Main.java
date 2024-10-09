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
package dev.roanh.wiki;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import net.dv8tion.jda.api.requests.GatewayIntent;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.DiscordBot;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.cmd.ClearNewsCommand;
import dev.roanh.wiki.cmd.MergeMasterCommand;
import dev.roanh.wiki.cmd.PreviewCommand;
import dev.roanh.wiki.cmd.RedateCommand;
import dev.roanh.wiki.cmd.RefreshCommand;
import dev.roanh.wiki.cmd.RestartCommand;
import dev.roanh.wiki.cmd.SwitchCommand;
import dev.roanh.wiki.exception.WebException;

/**
 * Main entry point of the application that starts the Discord bot.
 * @author Roan
 */
public class Main{
	/**
	 * Path to the osu! web wiki.
	 */
	public static final File WIKI_PATH = new File("/home/roan/wiki/osu-wiki");
	/**
	 * Path to the osu! web deploy key.
	 */
	public static final File AUTH_PATH = new File("/home/roan/wiki/auth");
	/**
	 * Path to the osu! web deploy instance root.
	 */
	public static final File DEPLOY_PATH = new File("/home/roan/wiki/deploy");
	/**
	 * Root domain for all instances.
	 */
	public static final String DOMAIN = "roanh.dev";
	/**
	 * The permission required to run wiki commands.
	 */
	public static final CommandPermission PERMISSION = CommandPermission.forRole(1109514462794358815L);//wiki role
	/**
	 * Discord bot instance.
	 */
	public static final DiscordBot client = new DiscordBot("/help", "!w", true, 569, 8999);
	/**
	 * Deployment instances.
	 */
	public static final Map<Long, OsuWeb> INSTANCES = Map.of(
		1145490143436873739L, new OsuWeb(1),
		1133099433853198427L, new OsuWeb(2),
		1145490162806173706L, new OsuWeb(3)
	);

	/**
	 * Starts the Discord bot.
	 * @param args No valid arguments.
	 */
	public static final void main(String[] args){
		try{
			OsuWiki.init();
		}catch(IOException e){
			client.logError(e, "[Main] Failed to initialise osu! wiki system.", Severity.MINOR, Priority.MEDIUM);
		}
		
		for(OsuWeb site : INSTANCES.values()){
			try{
				site.start();
			}catch(WebException | DBException e){
				client.logError(e, "[Main] Failed to start site with ID " + site.getID(), Severity.MINOR, Priority.MEDIUM);
			}
		}
		
		client.registerCommand(new SwitchCommand());
		client.registerCommand(new RestartCommand());
		client.registerCommand(new ClearNewsCommand());
		client.registerCommand(new RedateCommand());
		client.registerCommand(new RefreshCommand());
		client.registerCommand(new MergeMasterCommand());
		client.registerCommand(new PreviewCommand());
		
		client.addRequiredIntents(GatewayIntent.MESSAGE_CONTENT);
		client.login();
	}
}
