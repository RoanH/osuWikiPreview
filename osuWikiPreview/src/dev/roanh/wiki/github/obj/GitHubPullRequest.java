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
package dev.roanh.wiki.github.obj;

/**
 * Information about a GitHub pull request (PR).
 * @author Roan
 * @param id The GitHub internal pull request ID.
 * @param number The PR number as shown in the web UI.
 * @param state The state of this pull request.
 * @param title The title of this pull request.
 * @param user The user that created this pull request.
 * @param body The pull request description, can be null if absent.
 * @param head The branch associated with this pull request.
 * @param base The source branch the branch for this pull requested was based on.
 */
public record GitHubPullRequest(
		long id,
		int number,
		IssueState state,
		String title,
		GitHubUser user,
		String body,
		GitHubBranch head,
		GitHubBranch base
	){
	
	/**
	 * Checks if this pull request was opened on the official osu! wiki repository.
	 * @return True if this pull request was opened on the official wiki repository.
	 */
	public boolean isOnOfficialRepository(){
		return base.repo().isOfficial();
	}
}
