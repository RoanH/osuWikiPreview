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

import dev.roanh.wiki.github.hooks.PullRequestOpenData;

/**
 * Handler invoked when a pull request is created.
 * @author Roan
 * @see PullRequestOpenData
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads?actionType=opened#pull_request">Pull Request Opened Event</a>
 */
@FunctionalInterface
public abstract interface PullRequestOpenedHandler{

	/**
	 * Called when a pull request was opened.
	 * @param data Data about the newly opened pull request.
	 */
	public abstract void handlePullRequestOpen(PullRequestOpenData data);
}
