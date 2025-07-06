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
package dev.roanh.wiki;

import java.time.Duration;
import java.util.Optional;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.wiki.OsuWiki.SwitchResult;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.WebState;
import dev.roanh.wiki.exception.GitHubException;
import dev.roanh.wiki.exception.SwitchException;
import dev.roanh.wiki.github.GitHub;
import dev.roanh.wiki.github.obj.GitHubPullRequest;

public final class SwitchHelper{
	/**
	 * Default amount of time automatic claims last.
	 */
	private static final Duration DEFAULT_CLAIM_TIME = Duration.ofHours(1L);

	
	
	
	
	//TODO maybe delegate from osuweb?
	public static SwitchResult switchBranch(WebState target, OsuWeb web) throws SwitchException{
		
		
		
		
		
		
		
	}
	
	/**
	 * Switches the active preview branch.
	 * @param event The command event.
	 * @param state The state to switch to on the given web instance.
	 * @param web The osu! web instance to update.
	 * @param diff The git diff of the current state against ppy/master.
	 * @throws DBException When a database exception occurs.
	 */
	private void switchBranch(CommandEvent event, WebState state, OsuWeb web, SwitchResult diff) throws DBException{
		if(!state.isInternalBranch()){
			retrievePullRequest(state.getNamespace(), diff.head()).ifPresent(state::setPullRequest);//TODO needed?
		}
		
		state.refreshClaim(DEFAULT_CLAIM_TIME);
		web.setCurrentState(state);

		if(state.hasRedate() && diff.hasNews()){
			web.redateNews();
		}

		event.replyEmbeds(createEmbed(diff, state, web));
		InstanceStatus.updateOverview();
	}
	
	
	
	

	
	/**
	 * Determines the osu! web site path for the given file path
	 * inside the osu! wiki repository.
	 * @param repoPath The path of the markdown file in the osu! wiki
	 *        repository, this path is assumed to end with <code>.md</code>.
	 * @param instance The instance to resolve site paths with.
	 * @return The osu! web path for the given osu! wiki path or
	 *         <code>null</code> if the given path does not point to
	 *         a file that is visible on the website.
	 */
	private static final String resolveSitePath(String repoPath, Instance instance){
		int pathEnd = repoPath.lastIndexOf('/');
		if(pathEnd == -1){
			//some markdown file at the root of the repository
			return null;
		}
		
		String filename = repoPath.substring(pathEnd + 1, repoPath.length() - 3);
		if(repoPath.startsWith("news/")){
			return instance.getSiteUrl() + "/home/news/" + filename;
		}else if(repoPath.startsWith("wiki/Legal/")){
			if(pathEnd < 11){//root so no sub-path
				return instance.getSiteUrl() + "/legal/" + filename;
			}else{
				return instance.getSiteUrl() + "/legal/" + filename + "/" + repoPath.substring(11, pathEnd);
			}
		}else if(repoPath.startsWith("wiki/")){
			return instance.getSiteUrl() + "/wiki/" + filename + "/" + repoPath.substring(5, pathEnd);
		}else{
			return null;
		}
	}
	
	/**
	 * Attempts to retrieve pull request information for the given commit.
	 * @param namespace The namespace to look under.
	 * @param sha The commit hash to find.
	 * @return If found information about the pull requested associated with the commit.
	 * @throws GitHubException When a GitHub exception occurs.
	 */
	private static final Optional<GitHubPullRequest> retrievePullRequest(String namespace, String sha) throws GitHubException{
		return GitHub.instance().getPullRequestForCommit(namespace, sha);
	}
}
