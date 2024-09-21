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

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.exception.WebException;

/**
 * Command to restart the entire osu! web instance.
 * @author Roan
 */
public class RestartCommand extends WebCommand{

	/**
	 * Constructs a new restart command.
	 */
	public RestartCommand(){
		super("restart", "Restarts the entire osu! web instance.", Main.PERMISSION, true);
	}

	@Override
	public void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			web.stop();
			web.start();
			event.reply("osu! web instance succesfully restarted.");
		}catch(WebException | DBException e){
			event.logError(e, "[RestartCommand] Failed to restart osu! web instance", Severity.MINOR, Priority.MEDIUM);
			event.internalError();
		}
	}
}
