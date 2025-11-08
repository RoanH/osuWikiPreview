package dev.roanh.wiki.cmd;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandGroup;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.InstanceStatus;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.MainDatabase;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.cmd.WebCommand.WebCommandRunnable;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.data.UserGroup;

public class PrivateModeCommand extends CommandGroup{

	public PrivateModeCommand(){
		super("privatemode", "Manage private osu! wiki preview instances.");
		
		registerCommand(WebCommand.of("enable", "Enable private mode for this preview instance.", Main.PERMISSION, this::handleEnable));
		registerCommand(privateCommand("disable", "Disables private mode for this preview instance.", this::handleDisable));
		registerCommand(privateCommand("status", "Shows status information about this preview instance.", this::handleStatus));
		
		registerSubgroup(new GroupsCommand());
		registerSubgroup(new UsersCommand());
	}
	
	//TODO account for non-private mode
	
	private void handleEnable(OsuWeb web, CommandMap args, CommandEvent event){
		if(web.getInstance().isPrivateMode()){
			event.reply("This instance is already in private mode.");
		}else{
			//TODO
		}
	}
	
	private void handleDisable(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			Instance instance = web.getInstance();
			instance.clearAccessList();
			MainDatabase.saveInstance(instance);
			
			TextChannel chan = event.getJDA().getTextChannelById(instance.getChannel());
			chan.upsertPermissionOverride(chan.getGuild().getPublicRole()).clear(Permission.VIEW_CHANNEL).queue();
			
			event.reply("Private mode disabled successfully.");
			InstanceStatus.updateOverview();
		}catch(DBException e){
			event.logError(e, "[PrivateModeCommand] Failed to disable private mode", Severity.MINOR, Priority.MEDIUM, args);
			event.internalError();
		}
	}

	private void handleStatus(OsuWeb web, CommandMap args, CommandEvent event){

		
		
		
	}
	
	private static WebCommand privateCommand(String name, String description, WebCommandRunnable handler){
		return new WebCommand(name, description, Main.PERMISSION){
			
			@Override
			public void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
				if(!web.getInstance().isPrivateMode()){
					event.reply("This instance is not currently in private mode.");
					return;
				}
				
				handler.execute(web, args, event);
			}
		};
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
//			Main.client.getJDA().getGuildById(0).findMembers(null)
			
		}
		
		private void handleAdd(OsuWeb web, CommandMap args, CommandEvent event){
			
		}
		
		private void handleRemove(OsuWeb web, CommandMap args, CommandEvent event){
			
		}
	}
	
	private static class UsersCommand extends CommandGroup{

		public UsersCommand(){
			super("users", "Manage osu! users with access to this instance.");
			//TODO add/remove
		}
		
		private void handleAdd(OsuWeb web, CommandMap args, CommandEvent event){
			
		}
		
		private void handleRemove(OsuWeb web, CommandMap args, CommandEvent event){
			
		}
	}
}
