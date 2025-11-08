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

import java.util.Arrays;
import java.util.List;

import dev.roanh.osuapi.user.Group;

public enum UserGroup{
	GMT(1, "Global Moderation Team"),
	NAT(2, "Nomination Assessment Team"),
	BN(4, "Beatmap Nominators (incl. probationary)"),
	TC(8, "Tournament Committee"),
	LVD(16, "Project Loved"),
	BSC(32, "Beatmap Spotlight Curators");
	
	private final int id;
	private final String name;
	
	private UserGroup(int id, String name){
		this.id = id;
		this.name = name;
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public static List<String> getGroupNames(){
		return Arrays.stream(values()).map(UserGroup::getName).toList();
	}
	
	public static UserGroup from(String name){
		for(UserGroup group : values()){
			if(group.name().equalsIgnoreCase(name) || group.name.equalsIgnoreCase(name)){
				return group;
			}
		}
		
		return null;
	}
	
	public static UserGroup from(Group group){
		switch(group.asOsuGroup()){
		case GMT:
			return GMT;
		case NAT:
			return NAT;
		case BN:
		case BN_PROBATIONARY:
			return BN;
		case TC:
			return TC;
		case LVD:
			return LVD;
		case BSC:
			return BSC;
		case SPT:
		case DEV:
		case PPY://already part of the default set
		case FA:
		case DEFAULT:
		case BOT:
		case ANNOUNCE:
		case ALM:
			break;
		}
		
		return null;
	}
}
