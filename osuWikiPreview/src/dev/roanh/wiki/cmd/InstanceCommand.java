package dev.roanh.wiki.cmd;

import java.io.IOException;
import java.util.Optional;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandGroup;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.InstanceManager;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.data.Instance;

public class InstanceCommand extends CommandGroup{
	private static final long INSTANCES_CATEGORY = 1133099341079384165L;
	
	public InstanceCommand(){
		super("instance", "Creates and configures a new osu! web instance.");
		addOptionInt("id", "The identifier for the instance.");
		
		registerCommand(Command.of("env", "Regenerate the .env file for the given instance.", CommandPermission.DEV, false, this::generateEnv));
		
		Command create = Command.of("create", "Creates a new osu! web instance.", CommandPermission.DEV, false, this::createInstance);
		create.addOptionInt("port", "The web port for the new instance.", 1024, 65535);
		registerCommand(create);
	}

	public void generateEnv(CommandMap args, CommandEvent event){
		final int id = args.get("id").getAsInt();
		Optional<Instance> instance = Main.INSTANCES.values().stream().filter(web->web.getInstance().id() == id).map(OsuWeb::getInstance).findFirst();
		if(instance.isPresent()){
			try{
				new InstanceManager(instance.get()).generateEnv();
				event.reply("Environment regenerated successfully.");
			}catch(IOException e){
				event.logError(e, "[InstanceCommand] Failed to generate environment", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}
		}else{
			event.reply("Unknown instance");
		}
	}

	public void createInstance(CommandMap args, CommandEvent event){
		final int id = args.get("id").getAsInt();
		final int port = args.get("port").getAsInt();

		if(Main.INSTANCES.values().stream().map(OsuWeb::getInstance).anyMatch(instance->instance.id() == id || instance.port() == port)){
			event.reply("An instance with the given port or ID already exists.");
			return;
		}

		event.deferReply(deferred->{
			deferred.getJDA().getCategoryById(INSTANCES_CATEGORY).createTextChannel("osu" + id).queue(chan->{
				Instance instance = new Instance(id, chan.getIdLong(), port);
				
				
//				InstanceManager manager = new InstanceManager(INSTANCES_CATEGORY).create
					
					

			});
			
			
			
			
			
			
			
			
			
			
		});
	}
}
