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

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.db.DBContext;
import dev.roanh.osuapi.OsuAPI;
import dev.roanh.osuapi.Scope;
import dev.roanh.osuapi.session.OAuthSessionBuilder;

/**
 * General application configuration.
 * @author Roan
 * @param config The configuration file.
 * @param domain The root domain for all instances.
 */
public record Config(Configuration config, String domain){
	
	public Config(Configuration config){
		this(config, "preview.roanh.dev");
	}
	
	public OsuAPI getOsuAPI(){
		return OsuAPI.client(config.readInt("osu-client-id"), config.readString("osu-client-secret"));
	}

	public OAuthSessionBuilder getIdentitySessionBuilder(){
		return OsuAPI.oauth(config.readInt("osu-client-id"), config.readString("osu-client-secret")).setCallback("https://" + domain() + "/").addScopes(Scope.IDENTIFY);
	}

	public DBContext getMainDatabaseContext(){
		return getDatabaseContext("wikipreview");
	}
	
	public DBContext getDatabaseContext(String schema){
		return new DBContext(config.readString("db-url") + schema, "osuweb", config.readString("db-pass"));
	}
	
	public int getAuthServerPort(){
		return config.readInt("auth-port");
	}
	
	public int getLoginServerPort(){
		return config.readInt("login-port");
	}
}
