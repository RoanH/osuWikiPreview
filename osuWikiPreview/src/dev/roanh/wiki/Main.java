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

import java.io.IOException;

import net.dv8tion.jda.api.requests.GatewayIntent;

import dev.roanh.isla.DiscordBot;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.cmd.RestartCommand;
import dev.roanh.wiki.cmd.SwitchCommand;

/**
 * Main entry point of the application that starts the Discord bot.
 * @author Roan
 */
public class Main{
	/**
	 * The permission required to run wiki commands.
	 */
	public static final CommandPermission PERMISSION = CommandPermission.forRole(1109514462794358815L);//wiki role
	/**
	 * Discord bot instance.
	 */
	public static DiscordBot client;
	/**
	 * Legacy osu! web dev instance.
	 */
	@SuppressWarnings("deprecation")
	public static OsuWeb DEV_INSTANCE = new OsuWebDev();
	/**
	 * First osu! web deploy instance.
	 */
	public static OsuWeb OSU2 = new OsuWeb("https://osu2.roanh.dev/");

	/**
	 * Starts the Discord bot.
	 * @param args No valid arguments.
	 */
	public static final void main(String[] args){
		client = new DiscordBot("/help", "!w", true, 569, 8999);
		
		try{
			OsuWiki.init();
		}catch(IOException e){
			client.logError(e, "[Main] Failed to initialise osu! wiki system.", Severity.MINOR, Priority.MEDIUM);
		}
		
		client.registerCommand(new SwitchCommand());
		client.registerCommand(new RestartCommand());
		client.addRequiredIntents(GatewayIntent.MESSAGE_CONTENT);
		client.login();
	}
}
