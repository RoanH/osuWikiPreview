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

import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import dev.roanh.isla.command.slash.Command;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.OsuWiki;
import dev.roanh.wiki.OsuWiki.SwitchResult;

/**
 * Command to switch the active preview branch.
 * @author Roan
 */
public class SwitchCommand extends Command{
	
	/**
	 * Constructs a new osu! wiki command.
	 */
	public SwitchCommand(){
		super("switch", "Switch the preview site to a different branch.", Main.PERMISSION, true);
		addOptionString("ref", "The ref to switch to in the given name space (branch/hash/tag) or a GitHub style namespace:ref string.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRefs));
		addOptionOptionalString("namespace", "The user or organisation the osu-wiki fork is under, defaults to your Discord name.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRemotes));
	}
	
	@Override
	public void execute(CommandMap args, CommandEvent original){
		OsuWeb web;
		if(original.getChannelId() == 1133099410654498896L){
			web = Main.DEV_INSTANCE;
		}else if(original.getChannelId() == 1133099433853198427L){
			web = Main.OSU2;
		}else{
			original.reply("Please run this command in either <#1133099410654498896> (dev) or <#1133099433853198427> (deploy).");
			return;
		}
		
		if(web.tryLock()){
			original.reply("Already running an update, please try again later.");
			return;
		}
		
		final OsuWeb instance = web;
		original.deferReply(event->{
			try{
				String ref = args.get("ref").getAsString();
				String name = null;
				if(args.has("namespace")){
					name = args.get("namespace").getAsString();
				}else{
					int idx = ref.indexOf(':');
					if(idx <= 0 || idx == ref.length() - 1){
						name = original.getMember().getEffectiveName();
					}else{
						name = ref.substring(0, idx);
						ref = ref.substring(idx + 1, ref.length());
					}
				}
				
				SwitchResult diff = OsuWiki.switchBranch(name, ref, instance);
				
				EmbedBuilder embed = new EmbedBuilder();
				embed.setColor(new Color(255, 142, 230));
				embed.setAuthor("Ref: " + name + "/" + ref, "https://github.com/" + name + "/osu-wiki/tree/" + ref, null);
				embed.setFooter("HEAD: " + (diff.ff() ? (diff.head() + " (fast-forward)") : diff.head()));

				StringBuilder desc = embed.getDescriptionBuilder();
				for(DiffEntry item : diff.diff()){
					String path = resolveSitePath(item.getNewPath(), instance);
					if(path != null){
						int len = desc.length();
						desc.append("- [");
						desc.append(item.getNewPath());
						desc.append("](");
						desc.append(path);
						desc.append(")\n");
						if(desc.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH - "_more_".length()){
							desc.delete(len, desc.length());
							desc.append("_more_");
							break;
						}
					}
				}
				
				event.replyEmbeds(embed.build());
			}catch(InvalidRemoteException | NoRemoteRepositoryException ignore){
				event.reply("Could not find the wiki repository for the given namespace, is it named `osu-wiki`?");
			}catch(JGitInternalException ignore){
				if(ignore.getMessage().startsWith("Invalid ref name")){
					event.reply("Could not find the requested ref, does it exist?");
				}else{
					event.logError(ignore, "[SwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
					event.internalError();
				}
			}catch(Throwable e){
				event.logError(e, "[SwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}finally{
				instance.unlock();
			}
		});
	}
	
	/**
	 * Determines the osu! web site path for the given file path
	 * inside the osu! wiki repository.
	 * @param repoPath The path of the markdown file in the osu! wiki
	 *        repository, this path is assumed to end with <code>.md</code>.
	 * @param instance The osu! web instance to resolve site paths with.
	 * @return The osu! web path for the given osu! wiki path or
	 *         <code>null</code> if the given path does not point to
	 *         a file that is visible on the website.
	 */
	private static final String resolveSitePath(String repoPath, OsuWeb instance){
		int pathEnd = repoPath.lastIndexOf('/');
		if(pathEnd == -1){
			//some markdown file at the root of the repository
			return null;
		}
		
		String filename = repoPath.substring(pathEnd + 1, repoPath.length() - 3);
		if(repoPath.startsWith("news/")){
			return instance.getDomain() + "home/news/" + filename;
		}else if(repoPath.startsWith("wiki/Legal/")){
			return instance.getDomain() + "legal/" + filename + "/" + repoPath.substring(11, pathEnd);
		}else if(repoPath.startsWith("wiki/")){
			return instance.getDomain() + "wiki/" + filename + "/" + repoPath.substring(5, pathEnd);
		}else{
			return null;
		}
	}
}
