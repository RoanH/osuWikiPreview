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
	/**
	 * The instance access is being managed for.
	 */
	private final Instance instance;
	
	/**
	 * Constructs a new access manager.
	 * @param instance The instance to manage access for.
	 */
	protected AccessManager(Instance instance){
		this.instance = instance;
	}
	
	/**
	 * Enables private mode for the instance.
	 * @throws DBException When a database exception occurs.
	 */
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
	
	/**
	 * Disables private mode for the instance.
	 * @throws DBException When a database exception occurs.
	 */
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
	
	/**
	 * Checks if the given user is on the access for this instance
	 * and grants the relevant Discord permissions if so.
	 * @param user The user to sync access for.
	 * @throws IllegalStateException When the instance is not in private mode.
	 */
	public void syncAccess(User user) throws IllegalStateException{
		checkPrivate();
		
		if(instance.getAccessList().contains(user) && user.hasDiscord()){
			addRoleToUsers(List.of(user.discordId().getAsLong()));
		}
	}
	
	/**
	 * Adds a user to the instance.
	 * @param osuId The osu! ID of the user to add.
	 * @throws DBException When a database exception occurs.
	 * @throws IllegalStateException When the instance is not in private mode.
	 */
	public void addUser(int osuId) throws DBException, IllegalStateException{
		checkPrivate();
		
		instance.getAccessList().add(osuId);
		MainDatabase.saveInstance(instance);
		
		User user = MainDatabase.getUserById(osuId);
		if(user != null && user.hasDiscord()){
			addRoleToUsers(List.of(user.discordId().getAsLong()));
		}
	}
	
	/**
	 * Removes a user from the instance.
	 * @param osuId The osu! ID of the user to remove.
	 * @throws DBException When a database exception occurs.
	 * @throws IllegalStateException When the instance is not in private mode.
	 */
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
	
	/**
	 * Adds a user group to the instance.
	 * @param group The group to add.
	 * @throws DBException When a database exception occurs.
	 * @throws IllegalStateException When the instance is not in private mode.
	 */
	public void addGroup(UserGroup group) throws DBException, IllegalStateException{
		checkPrivate();
		
		instance.getAccessList().add(group);
		MainDatabase.saveInstance(instance);
		
		addRoleToUsers(MainDatabase.getUsersWithGroup(group.asGroupSet()).stream().filter(User::hasDiscord).map(user->{
			return user.discordId().getAsLong();
		}).toList());
	}
	
	/**
	 * Removes a user group from the instance.
	 * @param group The group to remove.
	 * @throws DBException When a database exception occurs.
	 * @throws IllegalStateException When the instance is not in private mode.
	 */
	public void removeGroup(UserGroup group) throws DBException, IllegalStateException{
		checkPrivate();
		
		AccessList acl = instance.getAccessList();
		acl.remove(group);
		MainDatabase.saveInstance(instance);
		
		removeRoleFromUsers(MainDatabase.getUsersWithGroup(group.asGroupSet()).stream().filter(User::hasDiscord).filter(user->!acl.contains(user)).map(user->{
			return user.discordId().getAsLong();
		}).toList());
	}
	
	/**
	 * Gets the Discord management channel for the instance.
	 * @return The Discord management channel for the instance.
	 */
	public TextChannel getChannel(){
		return Main.client.getJDA().getTextChannelById(instance.getChannel());
	}
	
	/**
	 * Gets the Discord access role for the management channel for the instance.
	 * @return The Discord access role for the instance.
	 */
	public Role getRole(){
		return Main.client.getJDA().getRoleById(instance.getRoleId());
	}
	
	/**
	 * Gets the management Discord guild.
	 * @return The management server.
	 */
	private Guild getGuild(){
		return getChannel().getGuild();
	}
	
	/**
	 * Adds the access role for the instance to the Discord users with the given IDs.
	 * @param users The discord users to add the role to.
	 */
	private void addRoleToUsers(Collection<Long> users){
		Guild guild = getGuild();
		Role role = getRole();
		for(long user : users){
			guild.addRoleToMember(UserSnowflake.fromId(user), role).queue();
		}
	}
	
	/**
	 * Removes the access role for the instance from the Discord users with the given IDs.
	 * @param users The discord users to remove the role from.
	 */
	private void removeRoleFromUsers(Collection<Long> users){
		Guild guild = getGuild();
		Role role = getRole();
		for(long user : users){
			guild.removeRoleFromMember(UserSnowflake.fromId(user), role).queue();
		}
	}
	
	/**
	 * Validates that the instance is currently in private mode.
	 * @throws IllegalStateException Thrown when the instance is not in private mode.
	 */
	private void checkPrivate() throws IllegalStateException{
		if(!instance.isPrivateMode()){
			throw new IllegalStateException("This instance is not currently in private mode.");
		}
	}
}
