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
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.errors.GitAPIException;

import net.dv8tion.jda.api.entities.Message.Attachment;

import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.isla.command.slash.CommandEvent;
import dev.roanh.isla.command.slash.CommandMap;
import dev.roanh.wiki.OsuWeb;
import dev.roanh.wiki.exception.WebException;

public class PreviewCommand extends BaseSwitchCommand{
	private static final Pattern POST_REGEX = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}-[a-zA-Z0-9-]+\\.md");
	
	/**
	 * Constructs a new refresh command command.
	 */
	public PreviewCommand(){
		super("preview", "Create a preview for the given newspost.");
		addOptionAttachment("newspost", "The newspost markdown file to preview.");
	}

	@Override
	public void handleSwitch(OsuWeb web, CommandMap args, CommandEvent event) throws GitAPIException, IOException, DBException, WebException{
		Attachment file = args.get("newspost").getAsAttachment();
		
		String name = file.getFileName();
		if(!name.endsWith(".md")){
			event.reply("The given file does not have the markdown (`.md`) extension.");
			return;
		}
		
		Matcher m = POST_REGEX.matcher(name);
		if(!m.matches()){
			event.reply("The name of the given file does not conform to the required format, see the [News styling criteria](https://osu.ppy.sh/wiki/en/News_styling_criteria).");
			return;
		}
		
		if(file.getSize() > 1024 * 1024 * 10){
			event.reply("The given newspost file is too large.");
			return;
		}
		
		try(InputStream in = file.getProxy().download().get()){
			pushBranch(event, web, args, in.readAllBytes(), Integer.parseInt(m.group(1)), m.group());
		}catch(InterruptedException e){
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}catch(ExecutionException e){
			throw new IOException(e);
		}
	}
}
