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

import java.io.File;
import java.io.IOException;

/**
 * Main control class for a dev osu! web instance.
 * @author Roan
 * @deprecated Legacy dev instance implementation.
 */
@Deprecated
public class OsuWebDev extends OsuWeb{
	
	public OsuWebDev(){
		super("https://osu.roanh.dev/");
	}
	
	@Override
	protected void runArtisan(String cmd) throws InterruptedException, IOException{
		runCommand("docker compose exec php /app/docker/development/entrypoint.sh artisan tinker --execute=\"" + cmd + "\"");
	}
	
	/**
	 * Runs a command to start the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	@Override
	public void start() throws InterruptedException, IOException{
		runCommand("bin/docker_dev.sh -d");
	}
	
	/**
	 * Runs a command to stop the osu! web instance.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	@Override
	public void stop() throws InterruptedException, IOException{
		runCommand("docker compose down --rmi all");
	}
	
	/**
	 * Runs a command on the server.
	 * @param cmd The command to run.
	 * @throws InterruptedException When the thread was interrupted.
	 * @throws IOException When an IOException occurs.
	 */
	private static void runCommand(String cmd) throws InterruptedException, IOException{
		new ProcessBuilder("bash", "-c", cmd).directory(new File("/home/roan/wiki/osu-web")).inheritIO().start().waitFor();
	}
}
