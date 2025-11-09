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

public class GroupSet implements Iterable<UserGroup>{
	private int groups;
	
	public GroupSet(){
		this(0);
	}
	
	protected GroupSet(int groups){
		this.groups = groups;
	}
	
	public void remove(UserGroup group){
		groups &= ~group.getId();
	}
	
	public void add(UserGroup group){
		groups |= group.getId();
	}
	
	public boolean containsAny(GroupSet other){
		return (groups & other.groups) != 0;
	}
	
	public int encode(){
		return groups;
	}
	
	public Set<UserGroup> getGroups(){
		return decodeGroups(groups);
	}
	
	@Override
	public Iterator<UserGroup> iterator(){
		return decodeGroups(groups).iterator();
	}
	
	public static Set<UserGroup> decodeGroups(int groups){
		Set<UserGroup> userGroups = EnumSet.noneOf(UserGroup.class);
		for(UserGroup group : UserGroup.values()){
			if((group.getId() & groups) != 0){
				userGroups.add(group);
			}
		}
		
		return userGroups;
	}
	
	public static GroupSet encodeGroups(Collection<? extends Group> groups){
		GroupSet set = new GroupSet();
		groups.stream().map(UserGroup::from).filter(Objects::nonNull).forEach(set::add);
		return set;
	}
}
