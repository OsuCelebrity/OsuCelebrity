package me.reddev.OsuCelebrity.Osu;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Responses;
import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Twitch.TwitchManager;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiUser;

@Slf4j
public class OsuIRCBot extends ListenerAdapter<PircBotX> implements Runnable
{
	public interface OsuIRCSettings {
		String getOsuIrcUsername();
		String getOsuIrcPassword();
		String getOsuPath();
	}
	
	private PircBotX _bot;
	
	private String _username;
	
	private TwitchManager _manager;
	private Downloader _downloader;
	
	/**
	 * Constructs a new Osu! IRC bot
	 * @param _manager The associated Twitch manager
	 * @param username The username of the Osu! IRC bot
	 * @param password The IRC password of the Osu! IRC bot
	 */
	public OsuIRCBot(Settings settings, TwitchManager manager)
	{
		_username = settings.getOsuIrcUsername();
		_manager = manager;
		_downloader = new Downloader(settings.getOsuApiKey());
		
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
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event)
	{
		String message = event.getMessage();
		//TODO: Accept in-game requests
		
		if(event.getUser().getNick().equalsIgnoreCase("Redback"))
		{
			OsuApiUser selectedUser;
			try {
				selectedUser = _downloader.getUser(event.getMessage(), GameModes.OSU, OsuApiUser.class);
			} catch (IOException e) {
				// I don't know what the plan should be in this case, but I didn't want the error to be unhandled --Tillerino
				
				log.warn("error getting user", e);
				selectedUser = null;
			}
			if(selectedUser == null)
			{
				event.getUser().send().message(String.format(Responses.INVALID_USER, event.getMessage()));
				return;
			}
			
			_manager.getRequests().AddRequest(selectedUser);
		}
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
