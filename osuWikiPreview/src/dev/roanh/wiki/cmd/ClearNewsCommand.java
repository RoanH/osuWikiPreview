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

/**
 * Command to clear the news posts database.
 * @author Roan
 */
public class ClearNewsCommand extends WebCommand{

	/**
	 * Constructs a new clear news command.
	 */
	public ClearNewsCommand(){
		super("clearnews", "Clears all generated news posts from the database.", Main.PERMISSION, true);
	}

	@Override
	public void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			web.clearNewsDatabase();
			event.reply("osu! web news database cleared succesfully.");
		}catch(DBException e){
			event.logError(e, "[ClearNewsCommand] Failed to clear news database", Severity.MINOR, Priority.MEDIUM);
			event.internalError();
		}
	}
}
