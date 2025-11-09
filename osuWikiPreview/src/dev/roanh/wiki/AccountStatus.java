package dev.roanh.wiki;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import dev.roanh.isla.DiscordBot;
import dev.roanh.wiki.auth.LoginServer;
import dev.roanh.wiki.auth.LoginServer.LoginInfo;
import dev.roanh.wiki.cmd.WebCommand;

/**
 * Simple manager for the account linking Discord channel.
 * @author Roan
 */
public final class AccountStatus{
	/**
	 * The channel with the Discord account linking embed.
	 */
	private static final long ACCOUNT_CHANNEL = 1436733000195899482L;
	
	/**
	 * Prevent instantiation.
	 */
	private AccountStatus(){
	}
	
	/**
	 * Initialises the account linking button handlers.
	 * @param bot The Discord bot instance.
	 * @param loginServer The login server to use.
	 */
	public static void init(DiscordBot bot, LoginServer loginServer){
		bot.registerButtonHandler("account:osu", (args, event)->handleLinking(event, loginServer));
	}
	
	/**
	 * Handles when someone tries to link their osu! account.
	 * @param event The button event.
	 * @param server The login server to use.
	 */
	private static void handleLinking(ButtonInteractionEvent event, LoginServer server){
		LoginInfo info = new LoginInfo(event.getUser().getIdLong());
		
		MessageCreateBuilder message = new MessageCreateBuilder();
		message.addContent("Please authorize your account on the following page:");
		message.addComponents(ActionRow.of(Button.link(server.createAuthUrl(info), "Open login page")));
		event.reply(message.build()).setEphemeral(true).queue();
	}
	
	/**
	 * Sends the embed(s) with information on account linking.
	 */
	public static void sendLinkEmbeds(){
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(WebCommand.THEME_COLOR);
		embed.setTitle("Link osu! account");
		embed.setDescription(
			"In order to manage private instances you have access to in this server you need to link your osu! and discord accounts. " +
			"If you only need to view private instances the on-site osu! login is sufficient and instances not in private mode can always be vieuwed even without logging in."
		);
		
		MessageCreateBuilder message = new MessageCreateBuilder();
		message.addEmbeds(embed.build());
		message.addComponents(ActionRow.of(Button.primary("account:osu", "Link osu! account")));
		
		Main.client.getJDA().getTextChannelById(ACCOUNT_CHANNEL).sendMessage(message.build()).queue();
	}
}
