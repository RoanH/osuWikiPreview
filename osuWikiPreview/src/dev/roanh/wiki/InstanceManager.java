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
import java.util.regex.Pattern;

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
	 * Format for osu! web docker image release tags.
	 */
	private static final Pattern RELEASE_TAG_REGEX = Pattern.compile("\\d{4}\\.\\d+\\.\\d+");
	/**
	 * Deployment instances by Discord channel.
	 */
	private static final Map<Long, OsuWeb> instancesByChannel = new HashMap<Long, OsuWeb>();
	/**
	 * The specific instance being managed by this manager.
	 */
	private final Instance instance;
	
	/**
	 * Constructs a new manager for the given instance.
	 * @param instance The instance to configure (possibly non-existant).
	 */
	public InstanceManager(Instance instance){
		this.instance = instance;
	}
	
	/**
	 * Creates a completely new instance except for the GitHub
	 * branch and network configuration.
	 * @throws DBException When a database exception occurs.
	 * @throws IOException When an IOException occurs.
	 * @throws WebException When an instance (docker) command fails.
	 */
	public void createInstance() throws DBException, IOException, WebException{
		MainDatabase.saveInstance(instance);
		generateEnv();
		prepareInstance();
		//technically should also push a new GitHub branch but I just made 9 in advance for now
		
		registerInstance(Main.client.getConfig(), instance);
	}
	
	/**
	 * Stops and delete the container for this instance.
	 * @throws WebException When a docker exception occurs.
	 */
	public void deleteInstanceContainer() throws WebException{
		Main.runCommand("docker stop " + instance.getWebContainer());
		Main.runCommand("docker rm " + instance.getWebContainer());
	}
	
	/**
	 * Runs a new docker container for the instance.
	 * @throws WebException When a docker exception occurs.
	 */
	public void runInstance() throws WebException{
		Main.runCommand("docker run -d --name " + instance.getWebContainer() + " --env-file " + instance.getEnvFile() + " -p " + instance.getPort() + ":8000 pppy/osu-web:" + instance.getTag() + " octane");
	}
	
	/**
	 * Prepare a newly created instance by running all seed/migration actions.
	 * @throws WebException When a docker exception occurs.
	 */
	private void prepareInstance() throws WebException{
		pullImageTag(instance.getTag());
		runArtisan("db:create");
		migrateInstance();
		runArtisan("es:index-documents");
		runArtisan("es:create-search-blacklist");
		runArtisan("es:index-wiki --create-only");
	}
	
	/**
	 * Deletes the current instance container and runs migrations
	 * to update to the given release tag.
	 * @param tag The new release tag for the instance.
	 * @throws WebException When a docker exception occurs.
	 * @throws DBException When a database exception occurs.
	 */
	public void updateInstance(String tag) throws WebException, DBException {
		pullImageTag(tag);
		instance.setTag(tag);
		MainDatabase.saveInstance(instance);
		deleteInstanceContainer();
		migrateInstance();
	}
	
	/**
	 * Runs migrations for this instance.
	 * @throws WebException When a docker exception occurs.
	 */
	private void migrateInstance() throws WebException{
		runArtisan("migrate --force");
	}

	/**
	 * Writes a new environment configuration for this instance.
	 * @throws IOException When an IOException occurs.
	 */
	public void generateEnv() throws IOException{
		Configuration config = new PropertiesFileConfiguration(Paths.get("secrets.properties"));
		try(PrintWriter out = new PrintWriter(Files.newBufferedWriter(Main.DEPLOY_PATH.toPath().resolve(instance.getEnvFile())))){
			out.println("# osu! web instance " + instance.getId());
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
			out.println("DB_DATABASE=" + instance.getDatabaseSchemaPrefix());
			out.println("DB_DATABASE_CHAT=" + instance.getDatabaseSchemaPrefix() + "_chat");
			out.println("DB_DATABASE_MP=" + instance.getDatabaseSchemaPrefix() + "_mp");
			out.println("DB_DATABASE_STORE=" + instance.getDatabaseSchemaPrefix() + "_store");
			out.println("DB_DATABASE_UPDATES=" + instance.getDatabaseSchemaPrefix() + "_updates");
			out.println("DB_DATABASE_CHARTS=" + instance.getDatabaseSchemaPrefix() + "_charts");
			out.println("DB_USERNAME=osuweb");
			out.println("DB_PASSWORD=" + config.readString("DB_PASSWORD"));
			out.println();
			out.println("# Redis");
			out.println("REDIS_HOST=" + config.readString("REDIS_HOST"));
			out.println("REDIS_PORT=6379");
			out.println("REDIS_DB=" + instance.getId());
			out.println("CACHE_REDIS_HOST=" + config.readString("REDIS_HOST"));
			out.println("CACHE_REDIS_PORT=6379");
			out.println("CACHE_REDIS_DB=" + instance.getId());
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
	
	/**
	 * Runs an artisan command for this instance in a new temporary container.
	 * @param cmd The artisan command to execute.
	 * @throws WebException When a docker exception occurs.
	 */
	private void runArtisan(String cmd) throws WebException{
		Main.runCommand("docker run --rm -t --env-file " + instance.getEnvFile() + " pppy/osu-web:" + instance.getTag() + " artisan " + cmd + " --no-interaction");
	}
	
	/**
	 * Pulls the given osu! web docker tag.
	 * @param tag The tag to pull.
	 * @throws WebException When a docker exception occurs,
	 *         or when the given tag is not a valid release tag.
	 */
	private static void pullImageTag(String tag) throws WebException{
		if(!RELEASE_TAG_REGEX.matcher(tag).matches()){
			throw new WebException("The given docker image tag '" + tag + "' does not look like a valid release tag.");
		}
		
		Main.runCommand("docker pull pppy/osu-web:" + tag);
	}
	
	/**
	 * Initialises the instance repository.
	 * @param config The application configuration file
	 * @throws DBException When a database exception occurs.
	 */
	public static void init(Configuration config) throws DBException{
		for(Instance instance : MainDatabase.getInstances()){
			registerInstance(config, instance);
		}
	}
	
	/**
	 * Gets an instance by the Discord channel that is used to manage it.
	 * @param channel The ID of the discord channel for this instance.
	 * @return The instance for the given channel if any, else null.
	 */
	public static OsuWeb getInstanceByChannel(long channel){
		return instancesByChannel.get(channel);
	}
	
	/**
	 * Returns a collection of all registered instances.
	 * @return A collection of all registered instances.
	 */
	public static Collection<OsuWeb> getInstances(){
		return instancesByChannel.values();
	}
	
	/**
	 * Registers a new instance.
	 * @param config The application configuration.
	 * @param instance The instance to register.
	 */
	private static void registerInstance(Configuration config, Instance instance){
		instancesByChannel.put(instance.getChannel(), new OsuWeb(config, instance));
	}
}
