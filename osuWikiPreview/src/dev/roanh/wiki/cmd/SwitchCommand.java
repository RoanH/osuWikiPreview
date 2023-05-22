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
package dev.roanh.wiki.cmd;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.diff.DiffEntry;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWiki;
import dev.roanh.wiki.OsuWiki.SwitchResult;

/**
 * Command to switch the active preview branch.
 * @author Roan
 */
public class SwitchCommand extends Command{
	/**
	 * Lock to present simultaneous command runs.
	 */
	private volatile AtomicBoolean busy = new AtomicBoolean(false);
	
	/**
	 * Constructs a new osu! wiki command.
	 */
	public SwitchCommand(){
		super("switch", "Switch the preview site to a different branch.", Main.PERMISSION, true);
		addOptionString("namespace", "The user or organisation the osu-wiki fork is under.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRemotes));
		addOptionString("ref", "The ref to switch to in the given name space (branch/hash/tag).", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRefs));
	}
	
	@Override
	public void execute(CommandMap args, CommandEvent original){
		if(busy.getAndSet(true)){
			original.reply("Already running an update, please try again later.");
			return;
		}
		
		original.deferReply(event->{
			try{
				String name = args.get("namespace").getAsString();
				String ref = args.get("ref").getAsString();
				SwitchResult diff = OsuWiki.switchBranch(name, ref);
				
				EmbedBuilder embed = new EmbedBuilder();
				embed.setColor(new Color(255, 142, 230));
				embed.setAuthor("Ref: " + ref, "https://github.com/" + name + "/osu-wiki/tree/" + ref, null);
				embed.setFooter("HEAD: " + (diff.ff() ? (diff.head() + " (fast-forward)") : diff.head()));

				StringBuilder desc = embed.getDescriptionBuilder();
				for(DiffEntry item : diff.diff()){
					int len = desc.length();
					desc.append("- [");
					desc.append(item.getNewPath());
					desc.append("](https://github.com/");
					desc.append(name);
					desc.append("/osu-wiki/blob/");
					desc.append(ref);
					desc.append('/');
					desc.append(item.getNewPath());
					desc.append(")\n");
					if(desc.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH - "_more_".length()){
						desc.delete(len, desc.length());
						desc.append("_more_");
						break;
					}
				}
				
				event.replyEmbeds(embed.build());
			}catch(Throwable e){
				event.logError(e, "[SwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}finally{
				busy.set(false);
			}
		});
	}
}
