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

/**
 * Command to switch the active preview branch.
 * @author Roan
 */
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
