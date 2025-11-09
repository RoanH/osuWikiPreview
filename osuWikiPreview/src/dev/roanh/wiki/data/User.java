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
package dev.roanh.wiki.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;

/**
 * User account record.
 * @author Roan
 * @param osuId The osu! ID of the user.
 * @param osuName The osu! name of the user.
 * @param discordId If known the Discord ID of the user.
 * @param groups The osu! user groups for the user.
 */
public record User(int osuId, String osuName, OptionalLong discordId, GroupSet groups){

	/**
	 * Constructs a new user from the given result set.
	 * @param rs The result set to read from.
	 * @throws SQLException when an SQL exception occurs.
	 */
	public User(ResultSet rs) throws SQLException{
		this(
			rs.getInt("osu"),
			rs.getString("username"),
			readLong(rs, "discord"),
			new GroupSet(rs.getInt("groups"))
		);
	}
	
	/**
	 * Checks if the Discord ID for this user is known.
	 * @return True if the Discord ID for this user is known.
	 */
	public boolean hasDiscord(){
		return discordId.isPresent();
	}
	
	/**
	 * Parses the given long column into an optional.
	 * @param rs The result set to read from.
	 * @param col The column to read.
	 * @return The long value if present, else an empty optional.
	 * @throws SQLException When an SQL exception occurs.
	 */
	private static OptionalLong readLong(ResultSet rs, String col) throws SQLException{
		long value = rs.getLong(col);
		return rs.wasNull() ? OptionalLong.empty() : OptionalLong.of(value);
	}
}
