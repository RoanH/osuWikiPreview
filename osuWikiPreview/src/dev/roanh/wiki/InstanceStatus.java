package dev.roanh.wiki;

import java.util.Comparator;
import java.util.Optional;

import net.dv8tion.jda.api.EmbedBuilder;

public class InstanceStatus{
	private static final long STATUS_CHANNEL = -1L;
	private static final long STATUS_MESSAGE = -1L;
	
	
	
	
	
	
	
	
	
	
	
	
	//instance / ref / pr / avail
	
	public static void updateOverview(){
		
		
		
		
		EmbedBuilder embed = new EmbedBuilder();
		
		
		
		StringBuilder instances = new StringBuilder();
		StringBuilder refs = new StringBuilder();
		StringBuilder available = new StringBuilder();
		InstanceManager.getInstances().stream().sorted(Comparator.comparing(w->w.getInstance().id())).forEach(web->{
			instances.append("<#");
			instances.append(web.getInstance().channel());
			instances.append(">\n");
			
			WebState state = web.getCurrentState();
			if(state != null){
				refs.append(state.getNamespaceWithRef());
				
				Optional<Long> pr = state.pr();
				if(pr.isPresent()){
					refs.append(" ([PR");
					refs.append(pr.)
				}
				
				
				refs.append(" (");
				refs.append("\n");
			}else{
				refs.append("None\n");
			}
			
			
			
			refs.append(web.getCurrentState().equals(available))
		});
		
		
	}
	
	
	
	
	
}
