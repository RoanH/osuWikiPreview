package dev.roanh.wiki;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import dev.roanh.infinity.config.Configuration;
import dev.roanh.infinity.config.PropertiesFileConfiguration;

public class InstanceGenerator{
	
	
	
	
	

	public static void generateEnv(int id) throws IOException{
		Configuration config = new PropertiesFileConfiguration(Paths.get("secrets.properties"));
		try(PrintWriter out = new PrintWriter(Files.newBufferedWriter(Main.DEPLOY_PATH.toPath().resolve("osu" + id + ".env")))){
			out.println("# osu! web instance " + id);
			out.println("APP_URL=https://osu" + id + ".roanh.dev");
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
			out.println("DB_DATABASE=osu" + id);
			out.println("DB_USERNAME=osuweb");
			out.println("DB_PASSWORD=" + config.readString("DB_PASSWORD"));
			out.println();
			out.println("# Redis");
			out.println("REDIS_HOST=" + config.readString("REDIS_HOST"));
			out.println("REDIS_PORT=6379");
			out.println("REDIS_DB=" + id);
			out.println("CACHE_REDIS_HOST=" + config.readString("REDIS_HOST"));
			out.println("CACHE_REDIS_PORT=6379");
			out.println("CACHE_REDIS_DB=" + id);
			out.println();
			out.println("# GitHub");
			out.println("GITHUB_TOKEN=" + config.readString("GITHUB_TOKEN"));
			out.println("WIKI_BRANCH=wikisync-" + id);
			out.println("WIKI_REPOSITORY=osu-wiki");
			out.println("WIKI_USER=RoanH");
			out.println();
			out.println("# Elasticsearch");
			out.println("ES_HOST=" + config.readString("ES_HOST") + ":9200");
			out.println("ES_INDEX_PREFIX=osu" + id);
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
}
