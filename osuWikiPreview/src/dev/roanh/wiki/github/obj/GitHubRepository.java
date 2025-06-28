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
 * Identifying information about a GitHub repository.
 * @author Roan
 * @param name The name of the GitHub repository.
 * @param owner The accounting that owns the repository.
 */
public record GitHubRepository(String name, GitHubUser owner){
	
	/**
	 * Checks if this is the official ppy osu! wiki GitHub repository.
	 * @return True if this repository is the official wiki repository.
	 */
	public boolean isOfficial(){
		return "osu-wiki".equals(name) && owner.id() == 995763;
	}
}
