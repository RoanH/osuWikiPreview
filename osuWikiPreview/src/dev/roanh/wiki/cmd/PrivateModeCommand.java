package dev.roanh.wiki.cmd;

import dev.roanh.isla.command.slash.CommandGroup;

public class PrivateModeCommand extends CommandGroup{

	public PrivateModeCommand(){
		super("privatemode", "Manage private osu! wiki preview instances.");
//		registerCommand(Command.of);
		registerSubgroup(new GroupsCommand());
		registerSubgroup(new UsersCommand());
	}
	
	//TODO enable
	//TODO disable
	//TODO status
	
	
	//pm enable:disable:groups add/remove:users add/remove:status

	private static class GroupsCommand extends CommandGroup{

		public GroupsCommand(){
			super("groups", "Manage osu! user groups with access to this instance.");
			//TODO add/remove
		}
	}
	
	private static class UsersCommand extends CommandGroup{

		public UsersCommand(){
			super("users", "Manage osu! users with access to this instance.");
			//TODO add/remove
		}
	}
}
