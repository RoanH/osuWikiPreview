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
package dev.roanh.wiki.github.handler;

import dev.roanh.wiki.github.hooks.IssueCommentCreatedData;

/**
 * Handler invoked when a comment is placed on an issue or a pull request.
 * @author Roan
 * @see IssueCommentCreatedData
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads?actionType=created#issue_comment">Issue Comment Created Event</a>
 */
@FunctionalInterface
public abstract interface IssueCommentHandler{
	
	/**
	 * Called when a new comment is created.
	 * @param data Data about the newly created comment.
	 */
	public abstract void handleComment(IssueCommentCreatedData data);
}
