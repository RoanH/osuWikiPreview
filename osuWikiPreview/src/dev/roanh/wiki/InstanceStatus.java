/*
 * osu! wiki preview site
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev) and contributors.
 * GitHub Repository: https://github.com/RoanH/osuWikiPreview
 * GitLab Repository: https://git.roanh.dev/roan/osuwikipreview
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.roanh.wiki;

import java.time.Instant;
import java.util.Comparator;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.TimeFormat;

import dev.roanh.wiki.cmd.WebCommand;
import dev.roanh.wiki.data.PullRequest;
import dev.roanh.wiki.data.WebState;

/**
 * Convenience overview of web instances and their status.
 * @author Roan
 */
public final class InstanceStatus{
	/**
	 * The status overview channel.
	 */
	private static final long STATUS_CHANNEL = 1350938353637396510L;
	/**
	 * The status message to update.
	 */
	private static final long STATUS_MESSAGE = 1350939456172982283L;

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
			InstanceManager.getInstances().stream().sorted(Comparator.comparing(w->w.getInstance().getId())).forEach(web->{
				instances.append("<#");
				instances.append(web.getInstance().getChannel());
				instances.append(">\n");
				
				if(web.getInstance().isPrivateMode()){
					refs.append("Private Mode\n");
					available.append("N/A");
				}else if(web.hasState()){
					WebState state = web.getCurrentState();
					refs.append("[");
					refs.append(state.getNamespaceWithRef());
					refs.append("](");
					refs.append(state.getGitHubTree());
					refs.append(")");
					
					if(state.hasPullRequest()){
						PullRequest pr = state.getPullRequest().orElseThrow();
						refs.append(" ([#");
						refs.append(pr.number());
						refs.append("](");
						refs.append(pr.getPrLink());
						refs.append("))");
					}
					
					refs.append("\n");
					available.append(TimeFormat.RELATIVE.format(state.getAvailableAt()));
				}else{
					refs.append("None\n");
					available.append(TimeFormat.RELATIVE.format(Instant.now()));
				}
				
				available.append("\n");
			});
			
			embed.addField("Instance", instances.toString(), true);
			embed.addField("State", refs.toString(), true);
			embed.addField("Available", available.toString(), true);
			
			msg.editMessageEmbeds(embed.build()).queue();
		});
	}
}
