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

/**
 * Basic GitHub pull request identification.
 * @author Roan
 * @param getId The globally unique ID of the pull request.
 * @param number The repository specific pull request number.
 */
public record PullRequest(long id, int number){

	/**
	 * Gets the complete web URL for this PR assuming it is in the official repository.
	 * @return The PR web URL.
	 */
	public String getPrLink(){
		return "https://github.com/ppy/osu-wiki/pull/" + number;
	}
}
