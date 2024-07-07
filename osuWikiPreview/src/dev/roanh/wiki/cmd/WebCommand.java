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

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;

/**
 * Abstract base class for command that affect an osu! web instance.
 * @author Roan
 */
public abstract class WebCommand extends Command{
	
	/**
	 * Constructs a new web command.
	 * @param name The name of this command.
	 * @param description The description of this command.
	 * @param permission The permission the user needs to be allowed to execute this command.
	 * @param guild True if this command only works in a guild.
	 */
	protected WebCommand(String name, String description, CommandPermission permission, boolean guild){
		super(name, description, permission, guild);
	}

	@Override
	public final void execute(CommandMap args, CommandEvent original){
		final OsuWeb web = Main.INSTANCES.getOrDefault(original.getChannelId(), null);
		if(web == null){
			original.reply("Please run this command in one of the channels under the `instances` category.");
			return;
		}
		
		original.deferReply(event->{
			try{
				if(web.tryLock()){
					original.reply("Already running a task, please try again later.");
					return;
				}
				
				executeWeb(web, args, event);
			}catch(Exception e){
				event.logError(e, "[WebCommand] Default failure", Severity.MAJOR, Priority.HIGH, args);
				event.internalError();
			}finally{
				web.unlock();
			}
		});
	}
	
	/**
	 * Executes this command for the given instance.
	 * @param web The osu! web instance to execute for.
	 * @param args The passed command arguments.
	 * @param event The command event.
	 */
	public abstract void executeWeb(OsuWeb web, CommandMap args, CommandEvent event);
}
