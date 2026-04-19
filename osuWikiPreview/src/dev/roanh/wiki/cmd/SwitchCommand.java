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
import java.util.Optional;

import org.eclipse.jgit.api.errors.GitAPIException;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.OsuWiki;
import dev.roanh.wiki.data.WebState;
import dev.roanh.wiki.exception.GitHubException;
import dev.roanh.wiki.exception.GitHubRepositoryOwnerNotFoundException;
import dev.roanh.wiki.exception.MergeConflictException;
import dev.roanh.wiki.exception.WebException;

/**
 * Command to switch the active preview branch.
 * @author Roan
 */
public class SwitchCommand extends BaseSwitchCommand{

	/**
	 * Constructs a new switch command.
	 */
	public SwitchCommand(){
		super("switch", "Switch the preview site to a different branch.");
		addOptionString("ref", "The ref to switch to in the given user namespace (branch/hash) or a namespace:ref string.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRefs));
		addOptionOptionalString("user", "The GitHub user/organisation the osu! wiki fork is under, defaults to your Discord name.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRemotes));
		addOptionOptionalBoolean("redate", "Redate future news to the current date (defaults to true).");
		addOptionOptionalBoolean("master", "If true ppy/master will be merged into the given target ref (defaults to false).");
	}
	
	@Override
	public void handleSwitch(OsuWeb web, CommandMap args, CommandEvent event) throws MergeConflictException, GitAPIException, IOException, DBException, WebException, GitHubException{
		String ref = args.get("ref").getAsString();
		String name = null;
		if(args.has("user")){
			name = args.get("user").getAsString();
		}else{
			int idx = ref.indexOf(':');
			if(idx <= 0 || idx == ref.length() - 1){
				name = event.getMember().getEffectiveName();
			}else{
				name = ref.substring(0, idx);
				ref = ref.substring(idx + 1, ref.length());
			}
		}

		try{
			Optional<String> repo = Main.githubAPI.getWikiFork(name);
			if(repo.isEmpty()){
				event.reply("Could not find an osu! wiki fork for the given GitHub user/organisation.");
				return;
			}

			switchBranch(
				event,
				WebState.forRef(
					name,
					repo.get(),
					ref,
					args.mapToBoolean("redate").orElse(true),
					args.mapToBoolean("master").orElse(false)
				),
				web,
				args
			);
		}catch(GitHubRepositoryOwnerNotFoundException ignore){
			event.reply("Could not find a GitHub user/organisation with the given name.");
		}
	}
}
