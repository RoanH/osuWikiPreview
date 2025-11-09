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

import java.util.Collection;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.wiki.data.AccessList;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.User;
import dev.roanh.wiki.data.UserGroup;

/**
 * Manager for the access list of a private instance.
 * @author Roan
 */
public class AccessManager{
	private final Instance instance;
	
	public AccessManager(Instance instance){
		this.instance = instance;
	}
	
	public void enablePrivateMode() throws DBException{
		instance.setAccessList(new AccessList());
		MainDatabase.saveInstance(instance);
		
		for(int id : instance.getAccessList().getUsers()){
			User user = MainDatabase.getUserById(id);
			if(user != null && user.hasDiscord()){
				addRoleToUsers(List.of(user.discordId().getAsLong()));
			}
		}

		TextChannel chan = getChannel();
		chan.upsertPermissionOverride(chan.getGuild().getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
	}
	
	public void disablePrivateMode() throws DBException{
		instance.clearAccessList();
		MainDatabase.saveInstance(instance);
		
		TextChannel chan = getChannel();
		chan.upsertPermissionOverride(chan.getGuild().getPublicRole()).clear(Permission.VIEW_CHANNEL).queue();
		
		Role role = getRole();
		Guild guild = getGuild();
		guild.findMembersWithRoles(role).onSuccess(members->{
			removeRoleFromUsers(members.stream().map(Member::getIdLong).toList());
		});
	}
	
	public void syncAccess(User user) throws IllegalStateException{
		checkPrivate();
		
		if(instance.getAccessList().contains(user) && user.hasDiscord()){
			addRoleToUsers(List.of(user.discordId().getAsLong()));
		}
	}
	
	public void addUser(int osuId) throws DBException, IllegalStateException{
		checkPrivate();
		
		instance.getAccessList().add(osuId);
		MainDatabase.saveInstance(instance);
		
		User user = MainDatabase.getUserById(osuId);
		if(user != null && user.hasDiscord()){
			addRoleToUsers(List.of(user.discordId().getAsLong()));
		}
	}
	
	public void removeUser(int osuId) throws DBException, IllegalStateException{
		checkPrivate();
		
		AccessList acl = instance.getAccessList();
		acl.remove(osuId);
		MainDatabase.saveInstance(instance);
		
		User user = MainDatabase.getUserById(osuId);
		if(user != null && user.hasDiscord() && !acl.contains(user)){
			removeRoleFromUsers(List.of(user.discordId().getAsLong()));
		}
	}
	
	public void addGroup(UserGroup group) throws DBException, IllegalStateException{
		checkPrivate();
		
		instance.getAccessList().add(group);
		MainDatabase.saveInstance(instance);
		
		addRoleToUsers(MainDatabase.getUsersWithGroup(group.asGroupSet()).stream().filter(User::hasDiscord).map(user->{
			return user.discordId().getAsLong();
		}).toList());
	}
	
	public void removeGroup(UserGroup group) throws DBException, IllegalStateException{
		checkPrivate();
		
		AccessList acl = instance.getAccessList();
		acl.remove(group);
		MainDatabase.saveInstance(instance);
		
		removeRoleFromUsers(MainDatabase.getUsersWithGroup(group.asGroupSet()).stream().filter(User::hasDiscord).filter(user->!acl.contains(user)).map(user->{
			return user.discordId().getAsLong();
		}).toList());
	}
	
	public TextChannel getChannel(){
		return Main.client.getJDA().getTextChannelById(instance.getChannel());
	}
	
	public Role getRole(){
		return Main.client.getJDA().getRoleById(instance.getRoleId());
	}
	
	private Guild getGuild(){
		return getChannel().getGuild();
	}
	
	private void addRoleToUsers(Collection<Long> users){
		Guild guild = getGuild();
		Role role = getRole();
		for(long user : users){
			guild.addRoleToMember(UserSnowflake.fromId(user), role).queue();
		}
	}
	
	private void removeRoleFromUsers(Collection<Long> users){
		Guild guild = getGuild();
		Role role = getRole();
		for(long user : users){
			guild.removeRoleFromMember(UserSnowflake.fromId(user), role).queue();
		}
	}
	
	private void checkPrivate() throws IllegalStateException{
		if(!instance.isPrivateMode()){
			throw new IllegalStateException("This instance is not currently in private mode.");
		}
	}
}
