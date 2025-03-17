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
 * Exception thrown for GitHub API issues.
 * @author Roan
 */
public final class GitHubException extends Exception{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 1202212041452857347L;
	
	/**
	 * Constructs a new GitHub exception with the given cause.
	 * @param cause The root cause.
	 */
	public GitHubException(Exception cause){
		super(cause);
	}
}