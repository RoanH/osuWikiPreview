package dev.roanh.wiki.cmd;

import java.io.IOException;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandGroup;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.exception.WebException;

public class InstanceCommand extends CommandGroup{
	private static final long INSTANCES_CATEGORY = 1133099341079384165L;
	
	public InstanceCommand(){
		super("instance", "Creates and configures a new osu! web instance.");
		addOptionInt("id", "The identifier for the instance.");
		
		registerCommand(WebCommand.of("env", "Regenerate the .env file for the given instance.", CommandPermission.DEV, this::generateEnv));
		
		registerCommand(WebCommand.of("recreate", "Creates the osu! web container again.", CommandPermission.DEV, this::recreateContainer));
		
		registerCommand(WebCommand.of("restart", "Runs a new container for the osu! web instance.", CommandPermission.DEV, this::runInstance));
		
		registerCommand(WebCommand.of("restart", "Restarts the entire osu! web instance.", CommandPermission.DEV, this::restartInstance));
		
		Command create = Command.of("create", "Creates a new osu! web instance.", CommandPermission.DEV, false, this::createInstance);
		create.addOptionInt("port", "The web port for the new instance.", 1024, 65535);
		registerCommand(create);
	}
	
	/**
	 * Command to restart the entire osu! web instance.
	 * @author Roan
	 * @param web The instance to restart.
	 * @param args The command arguments.
	 * @param event The command event.
	 */
	private void restartInstance(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			web.stop();
			web.start();
			event.reply("osu! web instance succesfully restarted.");
		}catch(WebException | DBException e){
			event.logError(e, "[InstanceCommand] Failed to restart osu! web instance", Severity.MINOR, Priority.MEDIUM);
			event.internalError();
		}
	}

	private void generateEnv(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			web.getManager().generateEnv();
			event.reply("Environment regenerated successfully (container still needs to be recreated).");
		}catch(IOException e){
			event.logError(e, "[InstanceCommand] Failed to generate environment", Severity.MINOR, Priority.MEDIUM, args);
			event.internalError();
		}
	}
	
	private void runInstance(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			web.getManager().runInstance();
			event.reply("osu! web instance successfully started.");
		}catch(WebException e){
			event.logError(e, "[InstanceCommand] Failed to run osu! web instance", Severity.MINOR, Priority.MEDIUM);
			event.internalError();
		}
	}
	
	private void recreateContainer(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			InstanceManager manager = web.getManager();
			manager.deleteInstanceContainer();
			manager.runInstance();
			event.reply("Instance recreated and started successfully.");
		}catch(WebException e){
			event.logError(e, "[InstanceCommand] Failed to generate environment", Severity.MINOR, Priority.MEDIUM, args);
			event.internalError();
		}
	}

	private void createInstance(CommandMap args, CommandEvent event){
		final int id = args.get("id").getAsInt();
		final int port = args.get("port").getAsInt();

		if(InstanceManager.getInstances().stream().map(OsuWeb::getInstance).anyMatch(instance->instance.id() == id || instance.port() == port)){
			event.reply("An instance with the given port or ID already exists.");
			return;
		}

		event.deferReply(deferred->{
			deferred.getJDA().getCategoryById(INSTANCES_CATEGORY).createTextChannel("osu" + id).queue(chan->{
				try{
					InstanceManager manager = new InstanceManager(new Instance(id, chan.getIdLong(), port));
					manager.createInstance();
					manager.runInstance();
					event.reply("Instance created succesfully (network configuration remains).");
				}catch(DBException | IOException | WebException e){
					event.logError(e, "[InstanceCommand] Failed to create instance", Severity.MINOR, Priority.MEDIUM, args);
					event.internalError();
				}
			});
		});
	}
}
