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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.config.PropertiesFileConfiguration;
import dev.roanh.infinity.db.concurrent.DBException;
import dev.roanh.wiki.data.Instance;
import dev.roanh.wiki.exception.WebException;

/**
 * General instance manager for administrative tasks.
 * @author Roan
 */
public class InstanceManager{
	/**
	 * Deployment instances by ID.
	 */
	private static final Map<Integer, OsuWeb> instancesById = new HashMap<Integer, OsuWeb>();
	/**
	 * Deployment instances by Discord channel.
	 */
	private static final Map<Long, OsuWeb> instancesByChannel = new HashMap<Long, OsuWeb>();
	/**
	 * The specific instance being managed by this manager.
	 */
	private final Instance instance;
	
	public InstanceManager(Instance instance){
		this.instance = instance;
	}
	
	public void createInstance() throws DBException, IOException, WebException{
		MainDatabase.addInstance(instance);
		generateEnv();
		MainDatabase.dropExtraSchemas();
		prepareInstance();
		//technically should also push a new GitHub branch but I just made 9 in advance for now
		
		registerInstance(Main.client.getConfig(), instance);
	}
	
	public void deleteInstanceContainer() throws WebException{
		Main.runCommand("docker stop " + instance.getWebContainer());
		Main.runCommand("docker rm " + instance.getWebContainer());
	}
	
	public void runInstance() throws WebException{
		Main.runCommand("docker run -d --name " + instance.getWebContainer() + " --env-file " + instance.getEnvFile() + " -p " + instance.port() + ":8000 pppy/osu-web:latest octane");
	}
	
	private void prepareInstance() throws WebException{
		runArtisan("db:create");
		runArtisan("migrate --force");
		runArtisan("es:index-documents");
		runArtisan("es:create-search-blacklist");
		runArtisan("es:index-wiki --create-only");
	}

	public void generateEnv() throws IOException{
		Configuration config = new PropertiesFileConfiguration(Paths.get("secrets.properties"));
		try(PrintWriter out = new PrintWriter(Files.newBufferedWriter(Main.DEPLOY_PATH.toPath().resolve(instance.getEnvFile())))){
			out.println("# osu! web instance " + instance.id());
			out.println("APP_URL=" + instance.getSiteUrl());
			out.println("APP_ENV=production");
			out.println("OCTANE_HTTPS=true");
			out.println("APP_DEBUG=false");
			out.println("APP_KEY=" + config.readString("APP_KEY"));
			out.println("APP_LOG_LEVEL=warning");
			out.println("CLIENT_CHECK_VERSION=false");
			out.println("ALLOW_REGISTRATION=false");
			out.println("IS_DEVELOPMENT_DEPLOY=true");
			out.println();
			out.println("# MySQL");
			out.println("DB_HOST=" + config.readString("DB_HOST"));
			out.println("DB_DATABASE=" + instance.getDatabaseSchema());
			out.println("DB_USERNAME=osuweb");
			out.println("DB_PASSWORD=" + config.readString("DB_PASSWORD"));
			out.println();
			out.println("# Redis");
			out.println("REDIS_HOST=" + config.readString("REDIS_HOST"));
			out.println("REDIS_PORT=6379");
			out.println("REDIS_DB=" + instance.id());
			out.println("CACHE_REDIS_HOST=" + config.readString("REDIS_HOST"));
			out.println("CACHE_REDIS_PORT=6379");
			out.println("CACHE_REDIS_DB=" + instance.id());
			out.println();
			out.println("# GitHub");
			out.println("GITHUB_TOKEN=" + config.readString("GITHUB_TOKEN"));
			out.println("WIKI_BRANCH=" + instance.getGitHubBranch());
			out.println("WIKI_REPOSITORY=osu-wiki");
			out.println("WIKI_USER=RoanH");
			out.println();
			out.println("# Elasticsearch");
			out.println("ES_HOST=" + config.readString("ES_HOST") + ":9200");
			out.println("ES_INDEX_PREFIX=" + instance.getElasticsearchPrefix());
			out.println();
			out.println("# Other");
			out.println("OSU_API_KEY=");
			out.println("BROADCAST_DRIVER=log");
			out.println("CACHE_DRIVER=file");
			out.println("SESSION_DRIVER=file");
			out.println("SLACK_ENDPOINT=https://myconan.net/null/");
			out.println("PUSHER_APP_ID=");
			out.println("PUSHER_KEY=");
			out.println("PUSHER_SECRET=");
			out.println("PAYMENT_SANDBOX=true");
			out.println("SHOPIFY_DOMAIN=");
			out.println("SHOPIFY_STOREFRONT_TOKEN=");
			out.println("SHOPIFY_WEBHOOK_KEY=");
			out.println("STORE_NOTIFICATION_CHANNEL=test");
			out.println("STORE_NOTIFICATIONS_QUEUE=store-notifications");
			out.println("STORE_STALE_DAYS=");
			out.println("PAYPAL_URL=https://www.sandbox.paypal.com/cgi-bin/webscr");
			out.println("PAYPAL_MERCHANT_ID=");
			out.println("PAYPAL_CLIENT_ID=");
			out.println("PAYPAL_CLIENT_SECRET=");
			out.println("PAYPAL_NO_SHIPPING_EXPERIENCE_PROFILE_ID=");
			out.println("XSOLLA_API_KEY=");
			out.println("XSOLLA_MERCHANT_ID=");
			out.println("XSOLLA_PROJECT_ID=");
			out.println("XSOLLA_SECRET_KEY=");
			out.println("CENTILI_API_KEY=");
			out.println("CENTILI_SECRET_KEY=");
			out.println("CENTILI_CONVERSION_RATE=");
			out.println("CENTILI_WIDGET_URL=https://api.centili.com/payment/widget");
			out.println("OSU_RUNNING_COST=");
		}
	}
	
	private void runArtisan(String cmd) throws WebException{
		Main.runCommand("docker run --rm -t --env-file " + instance.getEnvFile() + " pppy/osu-web:latest artisan " + cmd + " --no-interaction");
	}
	
	public static void init(Configuration config) throws DBException{
		for(Instance instance : MainDatabase.getInstances()){
			registerInstance(config, instance);
		}
	}
	
	public static OsuWeb getInstanceById(int id){
		return instancesById.get(id);
	}
	
	public static OsuWeb getInstanceByChannel(long channel){
		return instancesByChannel.get(channel);
	}
	
	
	public static Collection<OsuWeb> getInstances(){
		return instancesById.values();
	}
	
	private static void registerInstance(Configuration config, Instance instance){
		OsuWeb web = new OsuWeb(config, instance);
		instancesById.put(instance.id(), web);
		instancesByChannel.put(instance.channel(), web);
	}
}
