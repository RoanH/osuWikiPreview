package dev.roanh.wiki.cmd;

import java.io.IOException;

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
		
		registerCommand(Command.of("env", "Regenerate the .env file for the given instance.", CommandPermission.DEV, false, this::generateEnv));
		
		Command create = Command.of("create", "Creates a new osu! web instance.", CommandPermission.DEV, false, this::createInstance);
		create.addOptionInt("port", "The web port for the new instance.", 1024, 65535);
		registerCommand(create);
		
		registerCommand(Command.of("recreate", "Creates the osu! web container again.", CommandPermission.DEV, false, this::recreateContainer));
		
		//TODO start
	}

	public void generateEnv(CommandMap args, CommandEvent event){
		OsuWeb instance = InstanceManager.getInstanceById(args.get("id").getAsInt());
		if(instance != null){
			try{
				instance.getManager().generateEnv();
				event.reply("Environment regenerated successfully.");
			}catch(IOException e){
				event.logError(e, "[InstanceCommand] Failed to generate environment", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}
		}else{
			event.reply("Unknown instance");
		}
	}
	
	public void recreateContainer(CommandMap args, CommandEvent original){
		original.deferReply(event->{
			OsuWeb instance = InstanceManager.getInstanceById(args.get("id").getAsInt());
			if(instance != null){
				if(!instance.tryLock()){
					try{
						InstanceManager manager = instance.getManager();
						manager.deleteInstanceContainer();
						manager.runInstance();
						event.reply("Instance recreated and started successfully.");
					}catch(WebException e){
						event.logError(e, "[InstanceCommand] Failed to generate environment", Severity.MINOR, Priority.MEDIUM, args);
						event.internalError();
					}finally{
						instance.unlock();
					}
				}else{
					event.reply("Already running a task, please try again later.");
				}
			}else{
				event.reply("Unknown instance");
			}
		});
	}

	public void createInstance(CommandMap args, CommandEvent event){
		final int id = args.get("id").getAsInt();
		final int port = args.get("port").getAsInt();

		if(InstanceManager.getInstances().stream().map(OsuWeb::getInstance).anyMatch(instance->instance.id() == id || instance.port() == port)){
			event.reply("An instance with the given port or ID already exists.");
			return;
		}

		event.deferReply(deferred->{
			deferred.getJDA().getCategoryById(INSTANCES_CATEGORY).createTextChannel("osu" + id).queue(chan->{
				Instance instance = new Instance(id, chan.getIdLong(), port);
				
				
//				InstanceManager manager = new InstanceManager(INSTANCES_CATEGORY).create
					
					
				//TODO mention bot restart required to work + network config ufw+nginx
			});
			
			
			
			
			
			
			
			
			
			
		});
	}
}
