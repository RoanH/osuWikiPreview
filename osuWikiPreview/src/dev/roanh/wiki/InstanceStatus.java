package dev.roanh.wiki;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.TimeFormat;

import dev.roanh.wiki.cmd.WebCommand;

/**
 * Convenience overview of web instances and their status.
 * @author Roan
 */
public final class InstanceStatus{
	/**
	 * The status overview channel.
	 */
	private static final long STATUS_CHANNEL = 1350939456172982283L;
	/**
	 * The status message to update.
	 */
	private static final long STATUS_MESSAGE = 1350938353637396510L;

	/**
	 * Prevent instantiation.
	 */
	private InstanceStatus(){
	}
	
	/**
	 * Updates the status overview message.
	 */
	public static void updateOverview(){
		Main.client.getJDA().getTextChannelById(STATUS_CHANNEL).retrieveMessageById(STATUS_MESSAGE).queue(msg->{
			EmbedBuilder embed = new EmbedBuilder();
			embed.setColor(WebCommand.THEME_COLOR);
			embed.setFooter("Last updated");
			embed.setTimestamp(Instant.now());
			embed.setTitle("Instance Overview");
			
			StringBuilder instances = new StringBuilder();
			StringBuilder refs = new StringBuilder();
			StringBuilder available = new StringBuilder();
			InstanceManager.getInstances().stream().sorted(Comparator.comparing(w->w.getInstance().id())).forEach(web->{
				instances.append("<#");
				instances.append(web.getInstance().channel());
				instances.append(">\n");
				
				WebState state = web.getCurrentState();
				if(state != null){
					refs.append("[");
					refs.append(state.getNamespaceWithRef());
					refs.append("](");
					refs.append(state.getGitHubTree());
					refs.append(")");
					
					Optional<PullRequest> pr = state.getPullRequest();
					if(pr.isPresent()){
						refs.append(" ([PR");
						refs.append(pr.get().getPrLink());
						refs.append(")");
					}
					
					refs.append("\n");
					available.append(TimeFormat.RELATIVE.format(state.getAvailableAt()));
				}else{
					refs.append("None\n");
					available.append(Instant.now());
				}
			});
			
			embed.addField("Instance", instances.toString(), true);
			embed.addField("State", refs.toString(), true);
			embed.addField("Available", available.toString(), true);
			
			msg.editMessageEmbeds(embed.build()).queue();
		});
	}
}
