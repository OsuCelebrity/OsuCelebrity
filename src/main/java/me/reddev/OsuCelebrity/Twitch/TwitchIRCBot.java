package me.reddev.OsuCelebrity.Twitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Responses;
import me.reddev.OsuCelebrity.Logging.Logger;
import me.reddev.OsuCelebrity.Osu.OsuAPI;
import me.reddev.OsuCelebrity.Osu.OsuBeatmap;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;

public class TwitchIRCBot extends ListenerAdapter implements Runnable
{
	private PircBotX _bot;
	
	private String _channel, _username, _password;
	private List<String> _subscribers;
	
	/**
	 * Constructs a new Twitch IRC bot
	 * @param channel The username of the Twitch channel to connect to
	 * @param username The username of the Twitch IRC bot
	 * @param password The IRC password of the Twitch IRC bot
	 */
	public TwitchIRCBot(String channel, String username, String password)
	{
		_channel = channel;
		_username = username;
		_password = password;
		
		//Reset user lists
		_subscribers = new ArrayList<String>();
		
		//Reset bot
		Configuration config = new Configuration.Builder()
			.setName(_username)
			.setLogin(_username)
			.addListener(this)
			.setServer(Constants.TWITCH_IRC_HOST, Constants.TWITCH_IRC_PORT, _password)
			.setAutoReconnect(true)
			.addAutoJoinChannel(getChannel())
			.buildConfiguration();
		_bot = new PircBotX(config);
	}

	/**
	 * Connects to IRC and sets up listeners
	 */
	public void Start()
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
			Logger.Fatal("TwitchIRCBot IOException: "+ e.getMessage());
		} catch (IrcException e) {
			Logger.Fatal("TwitchIRCBot IrcException: "+ e.getMessage());
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
	public void SendMessage(GenericChannelEvent event, String message)
	{
		event.getBot().sendIRC().message(getChannel(), message);
	}
	
	private void CommandResponder(MessageEvent event, String message)
	{
		String[] messageSplit = message.substring(1).split(" ");
		String commandName = messageSplit[0];
		
		if(commandName.equalsIgnoreCase("request"))
		{
			OsuBeatmap selectedBeatmap = OsuAPI.GetBeatmap(Integer.parseInt(messageSplit[1]));
			if(selectedBeatmap == null)
			{
				SendMessage(event, String.format(Responses.INVALID_BEATMAP, messageSplit[1]));
				return;
			}
			
			TwitchManager.getRequests().AddRequest(selectedBeatmap);
			SendMessage(event, String.format(Responses.ADDED_TO_QUEUE, selectedBeatmap.toString()));
			SendMessage(event, String.format(Responses.CURRENT_QUEUE, TwitchManager.getRequests().getRequestCount()));
		}
		else if(commandName.equalsIgnoreCase("queue"))
		{
			TwitchRequest requests = TwitchManager.getRequests();
			SendMessage(event, String.format(Responses.NEXT_IN_QUEUE, requests.getRequestedBeatmaps().peek().toString()));
		}
	}
	
	private void ModCommandResponder(MessageEvent event, String message)
	{
		
	}

	// Listeners
	// http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html
	
	@Override
	public void onMessage(MessageEvent event)
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
	public void onPrivateMessage(PrivateMessageEvent event)
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
	public void onJoin(JoinEvent event)
	{
		if(event.getUser().getLogin().equalsIgnoreCase(_username))
		{
			//Ask for subscription and admin information
			event.getBot().sendRaw().rawLine("TWITCHCLIENT 3");
			Logger.Info(String.format("Joined %s", event.getChannel().getName()));
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
