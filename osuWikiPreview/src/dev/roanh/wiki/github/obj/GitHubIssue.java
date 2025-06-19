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

import com.google.gson.annotations.SerializedName;

/**
 * GitHub issue or pull request (those are also issues).
 * @author Roan
 * @param id The unique ID of the issue.
 * @param number The local repository specific issue/PR ID.
 * @param state The current state of the issue.
 * @param title The title of the issue.
 * @param pullRequest If this issue is a pull request,
 *        information about the pull request, else null.
 * @param user The user that created this issue/pr.
 */
public record GitHubIssue(
		long id,
		int number,
		IssueState state,
		String title,
		@SerializedName("pull_request")
		GitHubIssuePullRequest pullRequest,
		GitHubUser user
	){
	
	/**
	 * Checks if this issue represents a pull request.
	 * @return True if this issue is a pull request.
	 */
	public boolean isPullRequest(){
		return pullRequest != null;
	}
}
