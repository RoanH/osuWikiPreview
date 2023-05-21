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
