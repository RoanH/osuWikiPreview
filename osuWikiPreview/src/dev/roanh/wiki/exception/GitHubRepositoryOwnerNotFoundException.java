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
package dev.roanh.wiki.exception;

/**
 * Exception thrown when a requested GitHub user/organisation could not be found.
 * @author Roan
 */
public class GitHubRepositoryOwnerNotFoundException extends RuntimeException{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 8556652121895691187L;
	
	/**
	 * Constructs a new user/organisation not found exception.
	 * @param user The GitHub user/organisation that could not be found.
	 */
	public GitHubRepositoryOwnerNotFoundException(String user){
		super("No such user/organisation: " + user);
	}
}
