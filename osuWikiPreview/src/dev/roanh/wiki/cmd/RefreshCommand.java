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

/**
 * Command to refresh a preview with new commits.
 * @author Roan
 */
public class RefreshCommand extends SwitchCommand{

	/**
	 * Constructs a new refresh command command.
	 */
	public RefreshCommand(){
		super("refresh", "Switches to the mostly recently switched to ref.", Main.PERMISSION, true);
	}

	@Override
	public void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
		String ref = web.getCurrentRef();
		if(ref == null){
			event.reply("No previous ref found.");
		}else{
			String[] parts = ref.split("/");
			switchBranch(event, parts[1], parts[0], web, args);
		}
	}
}
