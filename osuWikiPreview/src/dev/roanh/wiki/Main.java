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

import net.dv8tion.jda.api.requests.GatewayIntent;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.DiscordBot;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.cmd.SyncNewsCommand;
import dev.roanh.wiki.cmd.InstanceCommand;
import dev.roanh.wiki.cmd.MergeMasterCommand;
import dev.roanh.wiki.cmd.NewsPreviewCommand;
import dev.roanh.wiki.cmd.RedateCommand;
import dev.roanh.wiki.cmd.RefreshCommand;
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
	public static final File WIKI_PATH = new File("osu-wiki").getAbsoluteFile();
	/**
	 * Path to the osu! web deploy key.
	 */
	public static final File AUTH_PATH = new File("auth").getAbsoluteFile();
	/**
	 * Path to the osu! web deploy instance root.
	 */
	public static final File DEPLOY_PATH = new File("deploy").getAbsoluteFile();
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
	 * Starts the Discord bot.
	 * @param args No valid arguments.
	 */
	public static final void main(String[] args){
		try{
			OsuWiki.init();
		}catch(IOException e){
			client.logError(e, "[Main] Failed to initialise osu! wiki system.", Severity.MINOR, Priority.MEDIUM);
		}
		
		try{
			MainDatabase.init(client.getConfig());
			InstanceManager.init(client.getConfig());
		}catch(DBException e){
			client.logError(e, "[Main] Failed to retrieve instances.", Severity.MINOR, Priority.MEDIUM);
		}
		
		for(OsuWeb site : InstanceManager.getInstances()){
			try{
				site.start();
			}catch(WebException | DBException e){
				client.logError(e, "[Main] Failed to start site with ID " + site.getInstance().getId(), Severity.MINOR, Priority.MEDIUM);
			}
		}
		
		client.registerCommand(new SwitchCommand());
		client.registerCommand(new SyncNewsCommand());
		client.registerCommand(new RedateCommand());
		client.registerCommand(new RefreshCommand());
		client.registerCommand(new MergeMasterCommand());
		client.registerCommand(new NewsPreviewCommand());
		client.registerCommand(new InstanceCommand());
		
		client.addRequiredIntents(GatewayIntent.MESSAGE_CONTENT);
		client.login();
	}
	
	/**
	 * Runs a command on the server.
	 * @param cmd The command to run.
	 * @throws WebException When an exception occurs.
	 */
	public static void runCommand(String cmd) throws WebException{
		try{
			int code = new ProcessBuilder("bash", "-c", cmd).directory(Main.DEPLOY_PATH).inheritIO().start().waitFor();
			if(0 != code){
				throw new IOException("Executed command returned exit code: " + code);
			}
		}catch(InterruptedException ignore){
			Thread.currentThread().interrupt();
			throw new WebException(ignore);
		}catch(IOException ignore){
			throw new WebException(ignore);
		}
	}
}
