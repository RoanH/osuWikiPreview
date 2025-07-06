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

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.isla.reporting.Priority;
import dev.roanh.isla.reporting.Severity;
import dev.roanh.wiki.InstanceStatus;
import dev.roanh.wiki.Main;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.OsuWiki;
import dev.roanh.wiki.OsuWiki.SwitchResult;
import dev.roanh.wiki.SwitchHelper;
import dev.roanh.wiki.data.PullRequest;
import dev.roanh.wiki.data.WebState;
import dev.roanh.wiki.exception.MergeConflictException;
import dev.roanh.wiki.exception.WebException;

/**
 * Base for commands that switch the active preview branch.
 * @author Roan
 */
public abstract class BaseSwitchCommand/* extends WebCommand*/{
	
	/**
	 * Constructs a new base switch command.
	 * @param name The name of this command.
	 * @param description The description of this command.
	 */
	protected BaseSwitchCommand(String name, String description){
		super(name, description, Main.PERMISSION);
	}
	
	@Override
	public final void executeWeb(OsuWeb web, CommandMap args, CommandEvent event){
		try{
			WebState target = handleSwitch(web, args, event);
			SwitchResult diff = SwitchHelper.switchBranch(target, web);
			event.replyEmbeds(createEmbed(diff, target, web));
			InstanceStatus.updateOverview();
		}catch(InvalidRemoteException | NoRemoteRepositoryException ignore){
			event.reply("Could not find the wiki repository for the given namespace, is it named `osu-wiki`?");
			ignore.printStackTrace();
		}catch(JGitInternalException ignore){
			if(ignore.getMessage().startsWith("Invalid ref name")){
				event.reply("Could not find the requested ref, does it exist?");
			}else{
				event.logError(ignore, "[BaseSwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
				event.internalError();
			}
		}catch(MergeConflictException ignore){
			event.reply("Failed to merge with ppy/master due to a merge conflict.");
		}catch(Exception e){
			event.logError(e, "[BaseSwitchCommand] Wiki update failed", Severity.MINOR, Priority.MEDIUM, args);
			event.internalError();
		}
	}
	
//	/**
//	 * Switches the active preview branch based on the user supplied arguments.
//	 * @param web The osu! web instance to update.
//	 * @param args The received input arguments.
//	 * @param event The command event.
//	 * @throws GitAPIException When a git exception occurs.
//	 * @throws IOException When an IOException occurs.
//	 * @throws DBException When a database exception occurs.
//	 * @throws WebException When a web exception occurs.
//	 * @throws MergeConflictException When a merge is requested which results in a conflict.
//	 */
	protected abstract WebState handleSwitch(OsuWeb web, CommandMap args, CommandEvent event) throws GitAPIException, IOException, DBException, WebException, MergeConflictException;
	
	/**
	 * Switches the active preview branch by pushing a file newspost file to ppy/master.
	 * @param event The command event.
	 * @param web The osu! web instance to update.
	 * @param args The received input arguments.
	 * @param data The newspost file content.
	 * @param year The year for the newspost.
	 * @param filename The filename of the newspost.
	 * @throws GitAPIException When a git exception occurs.
	 * @throws IOException When an IOException occurs.
	 * @throws DBException When a database exception occurs.
	 * @throws WebException When a web exception occurs.
	 */
	protected void pushBranch(CommandEvent event, OsuWeb web, CommandMap args, byte[] data, int year, String filename) throws GitAPIException, IOException, DBException, WebException{
		switchBranch(event, WebState.forNewspostPreview(web), web, OsuWiki.pushNews(data, year, filename, web));
	}
	
	/**
	 * Switches the active preview branch to the given web state.
	 * @param event The command event.
	 * @param state The state to switch to.
	 * @param web The osu! web instance to update.
	 * @param args The received input arguments.
	 * @throws GitAPIException When a git exception occurs.
	 * @throws IOException When an IOException occurs.
	 * @throws DBException When a database exception occurs.
	 * @throws WebException When a web exception occurs.
	 * @throws MergeConflictException When a merge is requested which results in a conflict.
	 */
	protected void switchBranch(CommandEvent event, WebState state, OsuWeb web, CommandMap args) throws MergeConflictException, GitAPIException, IOException, DBException, WebException{
		switchBranch(event, state, web, OsuWiki.switchBranch(state.getNamespace(), state.getRef(), state.hasMaster(), web));
	}
	
//	private void switchBranch();

	/**
	 * Switches the active preview branch.
	 * @param event The command event.
	 * @param state The state to switch to on the given web instance.
	 * @param web The osu! web instance to update.
	 * @param diff The git diff of the current state against ppy/master.
	 * @throws DBException When a database exception occurs.
	 */
	private void switchBranchOld(CommandEvent event, WebState state, OsuWeb web, SwitchResult diff) throws DBException{
		if(!state.isInternalBranch()){
			retrievePullRequest(state.getNamespace(), diff.head()).ifPresent(state::setPullRequest);
		}
		
		state.refreshClaim(DEFAULT_CLAIM_TIME);
		web.setCurrentState(state);

		if(state.hasRedate() && diff.hasNews()){
			web.redateNews();
		}

		event.replyEmbeds(createEmbed(diff, state, web));
		InstanceStatus.updateOverview();
	}
	
	/**
	 * Creates the reply embed with information about the switch.
	 * @param diff The git diff of the current state against ppy/master.
	 * @param state The new osu! web instance state.
	 * @param web The osu! web instance that was updated.
	 * @return The constructed switch result embed.
	 */
	private static final MessageEmbed createEmbed(SwitchResult diff, WebState state, OsuWeb web){
		String footer = "HEAD: " + diff.head();
		if(state.hasRedate() && diff.hasNews()){
			footer += state.hasMaster() ? " (with redate & master)" : " (with redate)";
		}else if(state.hasMaster()){
			footer += " (with master)";
		}
		
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(THEME_COLOR);
		embed.setAuthor("Ref: " + state.getNamespaceWithRef(), state.getGitHubTree(), null);
		embed.setFooter(footer);

		StringBuilder desc = embed.getDescriptionBuilder();
		
		if(state.hasPullRequest()){
			PullRequest pr = state.getPullRequest().orElseThrow();
			desc.append("Pull request [#");
			desc.append(pr.number());
			desc.append("](");
			desc.append(pr.getPrLink());
			desc.append(").\n");
		}
		
		for(DiffEntry item : diff.diff()){
			String path = resolveSitePath(item.getNewPath(), web.getInstance());
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
		
		return embed.build();
	}
}
