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

import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.WebState;

/**
 * Command to merge ppy/master into the current branch.
 * @author Roan
 */
public class MergeMasterCommand extends SwitchCommand{

	/**
	 * Constructs a new merge command command.
	 */
	public MergeMasterCommand(){
		super("mergemaster", "Merges ppy/master into the currently checked out branch.", Main.PERMISSION, true);
	}

	@Override
	public void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
		WebState state = web.getCurrentState();
		if(state == null){
			event.reply("No checked out ref found.");
		}else{
			switchBranch(event, state.withMaster(), web, args);
		}
	}
}
