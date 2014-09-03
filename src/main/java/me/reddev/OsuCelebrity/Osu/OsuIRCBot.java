package me.reddev.OsuCelebrity.Osu;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import me.reddev.OsuCelebrity.Constants.Constants;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;

@Slf4j
public class OsuIRCBot extends ListenerAdapter<PircBotX> implements Runnable
{
	public interface OsuIRCBotSettings {
		String getOsuIrcUsername();
		String getOsuIrcPassword();
	}

	private PircBotX _bot;
	
	private String _username;
	
	/**
	 * Constructs a new Osu! IRC bot
	 * @param username The username of the Osu! IRC bot
	 * @param password The IRC password of the Osu! IRC bot
	 */
	public OsuIRCBot(OsuIRCBotSettings settings)
	{
		_username = settings.getOsuIrcUsername();
		
		//Reset bot
		Configuration<PircBotX> config = new Configuration.Builder<PircBotX>()
			.setName(_username)
			.setLogin(_username)
			.addListener(this)
			.setServer(Constants.OSU_IRC_HOST, Constants.OSU_IRC_PORT, settings.getOsuIrcPassword())
			.setAutoReconnect(true)
			.buildConfiguration();
		_bot = new PircBotX(config);
	}

	/**
	 * Connects to IRC and sets up listeners
	 */
	public void start()
	{
		Thread botThread = new Thread(this);
		botThread.setName("OsuIRCBot");
		(botThread).start();
	}
	
	public void run()
	{
		try {
			_bot.startBot();
		} catch (IOException e) {
			log.error("OsuIRCBot IOException: "+ e.getMessage());
		} catch (IrcException e) {
			log.error("OsuIRCBot IrcException: "+ e.getMessage());
		}
	}

	/**
	 * Disconnects from the IRC server
	 */
	public void stop()
	{
		if (_bot.isConnected())
			_bot.stopBotReconnect();
	}

	/**
	 * Sends a message to the current IRC channel
	 * @param message The message to send to the channel
	 */
	public void sendMessage(GenericChannelEvent<PircBotX> event, String message)
	{
		event.getBot().sendIRC().message(event.getChannel().getName(), message);
	}

	public void sendCommand(String message)
	{
		_bot.sendIRC().message(Constants.OSU_COMMAND_USER, message);
	}
	
	// Listeners
	// http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event)
	{
		String message = event.getMessage();
		//TODO: Accept in-game requests
	}
	
	@Override
	public void onJoin(JoinEvent<PircBotX> event)
	{
		//Ask for subscription and admin information
		if(event.getUser().getLogin().equalsIgnoreCase(_username))
			log.info(String.format("Joined %s", event.getChannel().getName()));
	}
	
	// End Listeners
}
