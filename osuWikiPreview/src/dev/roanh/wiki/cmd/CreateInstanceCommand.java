package dev.roanh.wiki.cmd;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.permission.CommandPermission;

public class CreateInstanceCommand extends Command{
	
	public CreateInstanceCommand(){
		super("createinstance", "Creates and configures a new osu! web instance.", CommandPermission.DEV, false);
		addOptionOptionalBoolean("envonly", "Only generate a new .env nothing else.");
	}

	@Override
	public void execute(CommandMap args, CommandEvent event){
		
		
		
		
		// TODO Auto-generated method stub
		
	}

}
