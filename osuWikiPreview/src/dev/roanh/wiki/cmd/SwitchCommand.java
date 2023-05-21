package dev.roanh.wiki.cmd;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWiki;

public class SwitchCommand extends Command{
	/**
	 * Lock to present simultaneous command runs.
	 */
	private volatile AtomicBoolean busy = new AtomicBoolean(false);
	
	/**
	 * Constructs a new osu! wiki command.
	 */
	public SwitchCommand(){
		super("switch", "Switch the preview site to a different branch.", Main.PERMISSION, true);
		addOptionString("namespace", "The user or organisation the osu-wiki fork is under.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRemotes));
		addOptionString("ref", "The ref to switch to in the given name space (branch/hash/tag).", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRefs));
	}
	
	@Override
	public void execute(CommandMap args, CommandEvent original){
		if(busy.getAndSet(true)){
			original.reply("Already running an update, please try again later.");
			return;
		}
		
		original.deferReply(event->{
			try{
				OsuWiki.switchBranch(args.get("namespace").getAsString(), args.get("ref").getAsString(), event);
			}catch(Throwable e){
				event.logError(e, "[SwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}finally{
				busy.set(false);
			}
		});
	}
}
