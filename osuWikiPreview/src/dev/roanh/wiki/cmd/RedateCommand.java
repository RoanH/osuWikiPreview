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

import java.io.IOException;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;

/**
 * Command to redate news in the news posts database to the current date time.
 * @author Roan
 */
public class RedateCommand extends Command{

	/**
	 * Constructs a new redate news command.
	 */
	public RedateCommand(){
		super("redate", "Sets the published date for all news posts to the current date time.", Main.PERMISSION, true);
	}

	@Override
	public void execute(CommandMap args, CommandEvent original){
		OsuWeb web = Main.INSTANCES.getOrDefault(original.getChannelId(), null);
		if(web == null){
			original.reply("Please run this command in one of the channels under the `instances` category.");
			return;
		}
		
		original.deferReply(event->{
			try{
				web.redateNews();
				event.reply("News posts redated succesfully.");
			}catch(IOException | InterruptedException e){
				event.logError(e, "[RedateCommand] Failed to redate news", Severity.MINOR, Priority.MEDIUM);
				event.internalError();
			}
		});
	}
}
