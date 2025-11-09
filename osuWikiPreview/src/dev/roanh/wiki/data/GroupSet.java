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

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import dev.roanh.osuapi.user.Group;

/**
 * Compact representation of a set of osu! user groups.
 * @author Roan
 */
public class GroupSet implements Iterable<UserGroup>{
	/**
	 * Bitwise combination of group IDs.
	 */
	private int groups;

	/**
	 * Constructs a new empty group set.
	 */
	public GroupSet(){
		this(0);
	}

	/**
	 * Constructs a new group set with the given groups.
	 * @param groups Bitwise combination of contained group IDs.
	 */
	protected GroupSet(int groups){
		this.groups = groups;
	}

	/**
	 * Removes the given user group from the access list.
	 * @param group The group to remove.
	 */
	public void remove(UserGroup group){
		groups &= ~group.getId();
	}

	/**
	 * Adds the given user group to the access list.
	 * @param group The group to add.
	 */
	public void add(UserGroup group){
		groups |= group.getId();
	}

	/**
	 * Checks if this group set contains any of the given other groups.
	 * @param other The other groups to check.
	 * @return True if at least one of the other groups was found.
	 */
	public boolean containsAny(GroupSet other){
		return (groups & other.groups) != 0;
	}

	/**
	 * Encodes this group set as a single integer.
	 * @return The encoded form of this group set.
	 */
	public int encode(){
		return groups;
	}

	/**
	 * Gets all the groups contained in this group set.
	 * @return The groups contained in this group set.
	 */
	public Set<UserGroup> getGroups(){
		return decodeGroups(groups);
	}

	@Override
	public Iterator<UserGroup> iterator(){
		return decodeGroups(groups).iterator();
	}

	/**
	 * Constructs a new group set containing the given groups.
	 * <p>
	 * Note that groups are not copied one by one, the translation
	 * is governed by {@link UserGroup#from(Group)}.
	 * @param groups The groups to put in this set.
	 * @return The constructed group set.
	 * @see UserGroup
	 */
	public static GroupSet from(Collection<? extends Group> groups){
		GroupSet set = new GroupSet();
		groups.stream().map(UserGroup::from).filter(Objects::nonNull).forEach(set::add);
		return set;
	}

	/**
	 * Decodes the given bitwise group encoding into individual groups.
	 * @param groups The groups to decode.
	 * @return The decoded set of groups.
	 */
	private static Set<UserGroup> decodeGroups(int groups){
		Set<UserGroup> userGroups = EnumSet.noneOf(UserGroup.class);
		for(UserGroup group : UserGroup.values()){
			if((group.getId() & groups) != 0){
				userGroups.add(group);
			}
		}

		return userGroups;
	}
}
