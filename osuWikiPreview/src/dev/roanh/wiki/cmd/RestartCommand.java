package dev.roanh.wiki.cmd;

import java.io.IOException;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;

/**
 * Command to restart the entire osu! web instance.
 * @author Roan
 */
public class RestartCommand extends Command{

	/**
	 * Constructs a new restart command.
	 */
	public RestartCommand(){
		super("restart", "Restart the entire osu! web instance.", Main.PERMISSION, true);
	}

	@Override
	public void execute(CommandMap args, CommandEvent original){
		original.deferReply(event->{
			try{
				OsuWeb.stop();
				OsuWeb.start();
				event.reply("osu! web instance succesfully restarted.");
			}catch(IOException | InterruptedException e){
				event.logError(e, "[RestartCommand] Failed to restart osu! web instance", Severity.MINOR, Priority.MEDIUM);
				event.internalError();
			}
		});
	}
}
