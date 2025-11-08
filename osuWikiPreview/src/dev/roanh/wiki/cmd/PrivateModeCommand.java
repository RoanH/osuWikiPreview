package dev.roanh.wiki.cmd;

import java.util.StringJoiner;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandGroup;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.isla.reporting.Detail;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.osuapi.OsuAPI;
import dev.roanh.osuapi.exception.InsufficientPermissionsException;
import dev.roanh.osuapi.exception.RequestException;
import dev.roanh.osuapi.user.UserExtended;
import dev.roanh.wiki.InstanceStatus;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.MainDatabase;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.cmd.WebCommand.WebCommandRunnable;
import dev.roanh.wiki.data.AccessList;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.User;
import dev.roanh.wiki.data.UserGroup;

public class PrivateModeCommand extends CommandGroup{
	private final OsuAPI api;

	public PrivateModeCommand(OsuAPI api){
		super("privatemode", "Manage private osu! wiki preview instances.");
		this.api= api;
		
		registerCommand(WebCommand.of("enable", "Enable private mode for this preview instance.", Main.PERMISSION, this::handleEnable));
		registerCommand(privateCommand("disable", "Disables private mode for this preview instance.", this::handleDisable));
		registerCommand(privateCommand("status", "Shows status information about this preview instance.", this::handleStatus));
		
		registerSubgroup(new GroupsCommand());
		registerSubgroup(new UsersCommand());
	}
	
	private void handleEnable(OsuWeb web, CommandMap args, CommandEvent event){
		if(web.getInstance().isPrivateMode()){
			event.reply("This instance is already in private mode.");
		}else{
			try{
				Instance instance = web.getInstance();
				instance.setAccessList(new AccessList());
				MainDatabase.saveInstance(instance);

				TextChannel chan = event.getJDA().getTextChannelById(instance.getChannel());
				chan.upsertPermissionOverride(chan.getGuild().getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
				
				event.reply("Private mode enabled successfully.");
				InstanceStatus.updateOverview();
			}catch(DBException e){
				event.logError(e, "[PrivateModeCommand] Failed to enable private mode", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}
		}
	}
	
	private void handleDisable(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			Instance instance = web.getInstance();
			instance.clearAccessList();
			MainDatabase.saveInstance(instance);
			
			TextChannel chan = event.getJDA().getTextChannelById(instance.getChannel());
			chan.upsertPermissionOverride(chan.getGuild().getPublicRole()).clear(Permission.VIEW_CHANNEL).queue();
			
			Role role = event.getJDA().getRoleById(instance.getRoleId());
			event.getGuild().findMembersWithRoles(role).onSuccess(members->{
				for(Member member : members){
					event.getGuild().removeRoleFromMember(member, role).queue();
				}
			});
			
			event.reply("Private mode disabled successfully.");
			InstanceStatus.updateOverview();
		}catch(DBException e){
			event.logError(e, "[PrivateModeCommand] Failed to disable private mode", Severity.MINOR, Priority.MEDIUM, args);
			event.internalError();
		}
	}

	private void handleStatus(OsuWeb web, CommandMap args, CommandEvent event){
		AccessList acl = web.getInstance().getAccessList();
		
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(WebCommand.THEME_COLOR);
		embed.setTitle("Private Mode Status");
		
		StringJoiner groups = new StringJoiner(", ");
		acl.getGroups().forEach(group->groups.add(group.name()));
		embed.addField("Groups", groups.toString(), false);
		
		StringJoiner users = new StringJoiner(", ");
		acl.getUsers().forEach(user->users.add(String.valueOf(user)));
		embed.addField("Users", users.toString(), false);
		
		event.replyEmbeds(embed.build());
	}
	
	private static WebCommand privateCommand(String name, String description, WebCommandRunnable handler){
		return WebCommand.of(name, description, Main.PERMISSION, (web, args, event)->{
			if(!web.getInstance().isPrivateMode()){
				event.reply("This instance is not currently in private mode.");
				return;
			}

			handler.execute(web, args, event);
		});
	}
	
	private class UsersCommand extends CommandGroup{

		public UsersCommand(){
			super("users", "Manage osu! users with access to this instance.");
			
			WebCommand add = privateCommand("add", "Give a new osu! user access to this instance.", this::handleAdd);
			add.addOptionString("user", "The osu! user to add (username).", new SimpleAutoCompleteHandler(UserGroup.getGroupNames()));
			registerCommand(add);
			
			WebCommand remove = privateCommand("add", "Remove an osu! user's access to this instance.", this::handleRemove);
			remove.addOptionString("user", "The osu! user to remove (username).", new SimpleAutoCompleteHandler(UserGroup.getGroupNames()));
			registerCommand(remove);
		}
		
		private void handleAdd(OsuWeb web, CommandMap args, CommandEvent event){
			try{
				UserExtended osuUser = api.getUserByName(args.get("user").getAsString());
				if(osuUser == null){
					event.reply("Could not find the given user.");
					return;
				}
				
				Instance instance = web.getInstance();
				instance.getAccessList().add(osuUser.getId());
				MainDatabase.saveInstance(instance);
				
				User user = MainDatabase.getUserById(osuUser.getId());
				if(user != null && user.hasDiscord()){
					event.getGuild().addRoleToMember(
						UserSnowflake.fromId(user.discordId().getAsLong()),
						event.getJDA().getRoleById(instance.getRoleId())
					).queue();
				}
				
				event.reply("User added sucessfully.");
			}catch(InsufficientPermissionsException | RequestException | DBException e){
				event.logError(e, "[PrivateModeCommand] Failed to add user", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}
		}
		
		private void handleRemove(OsuWeb web, CommandMap args, CommandEvent event){
			try{
				UserExtended osuUser = api.getUserByName(args.get("user").getAsString());
				if(osuUser == null){
					event.reply("Could not find the given user.");
					return;
				}
				
				Instance instance = web.getInstance();
				instance.getAccessList().remove(osuUser.getId());
				MainDatabase.saveInstance(instance);
				
				User user = MainDatabase.getUserById(osuUser.getId());
				if(user != null && user.hasDiscord()){
					event.getGuild().removeRoleFromMember(
						UserSnowflake.fromId(user.discordId().getAsLong()),
						event.getJDA().getRoleById(instance.getRoleId())
					).queue();
				}
				
				event.reply("User removed sucessfully.");
			}catch(InsufficientPermissionsException | RequestException | DBException e){
				event.logError(e, "[PrivateModeCommand] Failed to add user", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}
		}
	}

	private static class GroupsCommand extends CommandGroup{

		public GroupsCommand(){
			super("groups", "Manage osu! user groups with access to this instance.");
			
			WebCommand add = privateCommand("add", "Give a new osu! user group access to this instance.", this::handleAdd);
			add.addOptionString("group", "The osu! user group to add.", new SimpleAutoCompleteHandler(UserGroup.getGroupNames()));
			registerCommand(add);
			
			WebCommand remove = privateCommand("add", "Remove an osu! user group's access to this instance.", this::handleRemove);
			remove.addOptionString("group", "The osu! user group to remove.", new SimpleAutoCompleteHandler(UserGroup.getGroupNames()));
			registerCommand(remove);
		}
		
		private void handleAdd(OsuWeb web, CommandMap args, CommandEvent event){
			UserGroup group = UserGroup.from(args.get("group").getAsString());
			if(group == null){
				event.reply("Could not find the given group.");
				return;
			}
			
			try{
				Instance instance = web.getInstance();
				instance.getAccessList().add(group);
				MainDatabase.saveInstance(instance);
				
				Role role = event.getJDA().getRoleById(instance.getRoleId());
				for(User user: MainDatabase.getUsersWithGroup(group.asGroupSet())){
					if(user.hasDiscord()){
						event.getGuild().addRoleToMember(UserSnowflake.fromId(user.discordId().getAsLong()), role).queue();
					}
				}
				
				event.reply("Group added sucessfully.");
			}catch(DBException e){
				event.logError(e, "[PrivateModeCommand] Failed to add group", Severity.MINOR, Priority.MEDIUM, args, Detail.of("group", group));
				event.internalError();
			}
		}
		
		private void handleRemove(OsuWeb web, CommandMap args, CommandEvent event){
			UserGroup group = UserGroup.from(args.get("group").getAsString());
			if(group == null){
				event.reply("Could not find the given group.");
				return;
			}
			
			try{
				Instance instance = web.getInstance();
				instance.getAccessList().remove(group);
				MainDatabase.saveInstance(instance);
				
				Role role = event.getJDA().getRoleById(instance.getRoleId());
				for(User user: MainDatabase.getUsersWithGroup(group.asGroupSet())){
					if(user.hasDiscord()){
						event.getGuild().removeRoleFromMember(UserSnowflake.fromId(user.discordId().getAsLong()), role).queue();
					}
				}
				
				event.reply("Group added sucessfully.");
			}catch(DBException e){
				event.logError(e, "[PrivateModeCommand] Failed to remove group", Severity.MINOR, Priority.MEDIUM, args, Detail.of("group", group));
				event.internalError();
			}
		}
	}
}
