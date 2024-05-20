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
import java.util.Optional;

import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.command.slash.SimpleAutoCompleteHandler;
import dev.roanh.isla.permission.CommandPermission;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.GitHub;
import dev.roanh.wiki.GitHub.GitHubException;
import dev.roanh.wiki.GitHub.PullRequestInfo;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.OsuWiki;
import dev.roanh.wiki.OsuWiki.MergeConflictException;
import dev.roanh.wiki.OsuWiki.SwitchResult;
import dev.roanh.wiki.WebState;

/**
 * Command to switch the active preview branch.
 * @author Roan
 */
public class SwitchCommand extends WebCommand{
	
	/**
	 * Constructs a new osu! wiki command.
	 */
	public SwitchCommand(){
		this("switch", "Switch the preview site to a different branch.", Main.PERMISSION, true);
		addOptionString("ref", "The ref to switch to in the given name space (branch/hash/tag) or a namespace:ref string.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRefs));
		addOptionOptionalString("namespace", "The user or organisation the osu-wiki fork is under, defaults to your Discord name.", 100, new SimpleAutoCompleteHandler(OsuWiki::getRecentRemotes));
		addOptionOptionalBoolean("redate", "Redate future news to the current date (defaults to true).");
		addOptionOptionalBoolean("master", "If true ppy/master will be merged into the given target ref (defaults to false).");
	}
	
	/**
	 * Constructs a new switch command.
	 * @param name The name of this command.
	 * @param description The description of this command.
	 * @param permission The permission the user needs to be allowed to execute this command.
	 * @param guild True if this command only works in a guild.
	 */
	protected SwitchCommand(String name, String description, CommandPermission permission, boolean guild){
		super(name, description, permission, guild);
	}

	@Override
	public void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
		String ref = args.get("ref").getAsString();
		String name = null;
		if(args.has("namespace")){
			name = args.get("namespace").getAsString();
		}else{
			int idx = ref.indexOf(':');
			if(idx <= 0 || idx == ref.length() - 1){
				name = event.getMember().getEffectiveName();
			}else{
				name = ref.substring(0, idx);
				ref = ref.substring(idx + 1, ref.length());
			}
		}

		switchBranch(event, new WebState(name, ref, args.mapToBoolean("redate").orElse(true), args.mapToBoolean("master").orElse(false)), web, args);
	}
	
	/**
	 * Switches the active preview branch.
	 * @param event The command event.
	 * @param state The state to switch to on the given web instance.
	 * @param web The osu! web instance to update.
	 * @param args The passed command arguments.
	 */
	protected void switchBranch(CommandEvent event, WebState state, OsuWeb web, CommandMap args){
		try{
			SwitchResult diff = OsuWiki.switchBranch(state.namespace(), state.ref(), state.master(), web);
			web.setCurrentState(state);
			
			String footer = "HEAD: " + diff.head();
			if(state.redate() && diff.hasNews()){
				web.redateNews();
				footer += state.master() ? " (with redate & master)" : " (with redate)";
			}else if(state.master()){
				footer += " (with master)";
			}
			
			EmbedBuilder embed = new EmbedBuilder();
			embed.setColor(new Color(255, 142, 230));
			embed.setAuthor("Ref: " + state.namespace() + "/" + state.ref(), state.getGitHubTree(), null);
			embed.setFooter(footer);

			StringBuilder desc = embed.getDescriptionBuilder();
			
			try{
				Optional<PullRequestInfo> pr = GitHub.getPullRequestsForCommit(state.namespace(), diff.head());
				if(pr.isPresent()){
					PullRequestInfo info = pr.get();
					desc.append("[PR#");
					desc.append(info.number());
					desc.append("](");
					desc.append(info.getUrl());
					desc.append(")\n");
				}
			}catch(GitHubException ignore){
				//missing PR info should not hold back the embed
			}
			
			for(DiffEntry item : diff.diff()){
				String path = resolveSitePath(item.getNewPath(), web);
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
		}catch(MergeConflictException ignore){
			event.reply("Failed to merge with ppy/master due to a merge conflict.");
		}catch(Exception e){
			event.logError(e, "[SwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
			event.internalError();
		}
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
			if(pathEnd < 11){//root so no sub-path
				return instance.getDomain() + "legal/" + filename;
			}else{
				return instance.getDomain() + "legal/" + filename + "/" + repoPath.substring(11, pathEnd);
			}
		}else if(repoPath.startsWith("wiki/")){
			return instance.getDomain() + "wiki/" + filename + "/" + repoPath.substring(5, pathEnd);
		}else{
			return null;
		}
	}
}
