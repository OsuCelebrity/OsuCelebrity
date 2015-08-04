package me.reddev.OsuCelebrity.Twitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Responses;

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
public class TwitchIRCBot extends ListenerAdapter<PircBotX> implements Runnable
{
	public interface TwitchIrcSettings {
		String getTwitchIrcChannel();
		String getTwitchIrcUsername();
		String getTwitchToken();
	}
	
	private PircBotX _bot;
	
	private String _channel, _username;
	private List<String> _subscribers;

	private TwitchManager _twitchManager;

	private Downloader _downloader;
	
	/**
	 * Constructs a new Twitch IRC bot
	 * @param channel The username of the Twitch channel to connect to
	 * @param username The username of the Twitch IRC bot
	 * @param password The IRC password of the Twitch IRC bot
	 */
	public TwitchIRCBot(TwitchIrcSettings settings, TwitchManager twitchManager, Downloader downloader)
	{
		_channel = settings.getTwitchIrcChannel();
		_username = settings.getTwitchIrcUsername();
		
		//Reset user lists
		_subscribers = new ArrayList<String>();
		
		//Reset bot
		Configuration<PircBotX> config = new Configuration.Builder<PircBotX>()
			.setName(_username)
			.setLogin(_username)
			.addListener(this)
			.setServer(Constants.TWITCH_IRC_HOST, Constants.TWITCH_IRC_PORT, settings.getTwitchToken())
			.setAutoReconnect(true)
			.addAutoJoinChannel(getChannel())
			.buildConfiguration();
		_bot = new PircBotX(config);
		
		this._twitchManager = twitchManager;
		this._downloader = downloader;
	}

	/**
	 * Connects to IRC and sets up listeners
	 */
	public void start()
	{
		Thread botThread = new Thread(this);
		botThread.setName("TwitchIRCBot");
		(botThread).start();
	}
	
	public void run()
	{
		try {
			_bot.startBot();
		} catch (IOException e) {
			log.error("TwitchIRCBot IOException: "+ e.getMessage());
		} catch (IrcException e) {
			log.error("TwitchIRCBot IrcException: "+ e.getMessage());
		}
	}

	/**
	 * Disconnects from the IRC server
	 */
	public void Stop()
	{
		if (_bot.isConnected())
			_bot.stopBotReconnect();
	}

	/**
	 * Sends a message to the current IRC channel
	 * @param message The message to send to the channel
	 */
	public void sendMessage(String message)
	{
		_bot.sendIRC().message(getChannel(), message);
	}
	
	private void CommandResponder(MessageEvent<PircBotX> event, String message)
	{
		String[] messageSplit = message.substring(1).split(" ");
		String commandName = messageSplit[0];
		
		if(commandName.equalsIgnoreCase("queue"))
		{
			//Missing supporting arguments
			if(messageSplit.length < 2)
			{
				sendMessage(Responses.INVALID_FORMAT_QUEUE);
				return;
			}
			
			OsuApiUser selectedUser;
			try {
				selectedUser = _downloader.getUser(messageSplit[1], GameModes.OSU, OsuApiUser.class);
			} catch (IOException e) {
				// I don't know what the plan should be in this case, but I didn't want the error to be unhandled --Tillerino
				
				log.warn("error getting user", e);
				selectedUser = null;
			}
			if(selectedUser == null)
			{
				sendMessage(String.format(Responses.INVALID_USER, messageSplit[1]));
				return;
			}
			
			_twitchManager.addRequest(selectedUser);
		}
		else if(commandName.equalsIgnoreCase("next"))
		{
			TwitchRequest requests = _twitchManager.getRequests();
			sendMessage(String.format(Responses.NEXT_IN_QUEUE, requests.getRequestedUsers().peek().toString()));
		}
	}
	
	private void ModCommandResponder(MessageEvent<PircBotX> event, String message)
	{
		
	}

	// Listeners
	// http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event)
	{
		String message = event.getMessage();
		//Search through for command calls
		if(message.startsWith(String.valueOf(Constants.TWITCH_IRC_COMMAND)))
		{
			if(event.getChannel().isOp(event.getUser()))
				ModCommandResponder(event, message);
			CommandResponder(event, message);
		}
	}
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event)
	{
		String message = event.getMessage();
		Pattern specialUserRegex = 
				Pattern.compile("SPECIALUSER ([^ ]+) (subscriber|turbo)");
		Matcher matches = specialUserRegex.matcher(message);
		
		//Nothing important - no special messages
		if(!matches.matches()) return;
		
		if(matches.group(2).equalsIgnoreCase("subscriber"))
			_subscribers.add(matches.group(1).toLowerCase());
		else
		{
			//TODO: Add a special case for turbo users
		}
	}
	
	@Override
	public void onJoin(JoinEvent<PircBotX> event)
	{
		if(event.getUser().getLogin().equalsIgnoreCase(_username))
		{
			//Ask for subscription and admin information
			event.getBot().sendRaw().rawLine("TWITCHCLIENT 3");
			log.info(String.format("Joined %s", event.getChannel().getName()));
		}
	}
	
	// End Listeners

	/**
	 * @return The list of subscribers in the IRC channel
	 */
	public List<String> getSubscribers()
	{
		return _subscribers;
	}
	
	/**
	 * Gets the IRC channel of the IRC bot
	 * @return The IRC channel with pound symbol
	 */
	public String getChannel()
	{
		return String.format("#%s", _channel.toLowerCase());
	}
}
