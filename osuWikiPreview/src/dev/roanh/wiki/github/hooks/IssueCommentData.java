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
package dev.roanh.wiki.github.hooks;

import dev.roanh.wiki.github.obj.GitHubComment;
import dev.roanh.wiki.github.obj.GitHubIssue;

/**
 * Web hook data for an issue comment (PRs are also issues).
 * @author Roan
 * @param action The comment event type, one of 'created', 'deleted', and 'edited'.
 * @param comment Information about the comment.
 * @param issue Information about the issue the comment is on.
 */
public record IssueCommentData(String action, GitHubComment comment, GitHubIssue issue){
	
	/**
	 * Checks if this event is for a newly created comment.
	 * @return True if this event is for a newly created comment.
	 */
	public boolean isCreateAction(){
		return "created".equals(action);
	}
}
