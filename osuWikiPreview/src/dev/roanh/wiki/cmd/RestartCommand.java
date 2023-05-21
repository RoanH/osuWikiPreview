package dev.roanh.wiki.cmd;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.wiki.Main;

public class RestartCommand extends Command{

	public RestartCommand(){
		super("restart", "Restart the entire osu! web instance.", Main.PERMISSION, true);
	}

	@Override
	public void execute(CommandMap arg0, CommandEvent arg1){
		// TODO Auto-generated method stub
		
	}
}
