package me.reddev.OsuCelebrity.Twitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Logging.Logger;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

public class TwitchIRCBot extends PircBot
{
	private String _channel, _username, _password;
	private List<String> _moderators, _subscribers, _users;
	
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
		_moderators = new ArrayList<String>();
		_subscribers = new ArrayList<String>();
		_users = new ArrayList<String>();
	}

	/**
	 * Connects to IRC and sets up listeners
	 * @throws IOException Issues connecting to the server
	 * @throws IrcException Issues connecting within IRC
	 */
	public void Start() throws IOException, IrcException
	{
		setName(_username);
		setLogin(_username);

		this.setVerbose(Constants.DEBUGGING);
		this.connect(Constants.IRC_HOST, Constants.IRC_PORT, _password);
		
		this.joinChannel(getChannel());
	}

	/**
	 * Disconnects from the IRC server
	 */
	public void Stop()
	{
		if (isConnected())
			disconnect();
	}

	/**
	 * Sends a message to the current IRC channel
	 * @param message The message to send to the channel
	 */
	public void SendMessage(String message)
	{
		sendMessage(getChannel(), message);
	}

	// Listeners
	
	@Override
	protected void onMessage(String channel, String sender,
			String login, String hostname, String message)
	{
		//Search through for command calls
		if(message.startsWith(String.valueOf(Constants.IRC_COMMAND)))
		{
			
		}
	}
	
	@Override
	protected void onUserMode(String targetNick, String sourceNick,
			String sourceLogin, String sourceHostname, String mode)
	{
		if(mode.equalsIgnoreCase("-o") && 
				_moderators.contains(targetNick.toLowerCase()))
			_moderators.remove(targetNick.toLowerCase());
		else if(mode.equalsIgnoreCase("+o"))
			_moderators.add(targetNick.toLowerCase());
	}
	
	@Override
	protected void onPrivateMessage(String sender, String login, 
			String hostname, String message)
	{
		Pattern specialUserRegex = 
				Pattern.compile("SPECIALUSER [^ ]+ (subscriber|turbo)");
		Matcher matches = specialUserRegex.matcher(message);
		
		//Nothing important - no special messages
		if(!matches.matches()) return;
		
		if(matches.group(1).equalsIgnoreCase("subscriber"))
			_subscribers.add(sender);
		else
		{
			//TODO: Add a special case for turbo users
		}
		
		Logger.Info(message);
	}

	@Override
	protected void onPart(String channel, String sender, String login,
			String hostname)
	{
		if(_users.contains(sender))
			_users.remove(sender);
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login,
			String hostname)
	{
		//Ask for subscription and admin information
		this.sendRawLine("TWITCHCLIENT 3");
		if(sender.equalsIgnoreCase(_username))
			Logger.Info(String.format("Joined %s", channel));
		_users.add(sender);
	}
	
	// End Listeners
	
	/**
	 * @return The list of moderators in the IRC channel
	 */
	public List<String> getModerators()
	{
		return _moderators;
	}

	/**
	 * @return The list of subscribers in the IRC channel
	 */
	public List<String> getSubscribers()
	{
		return _subscribers;
	}

	/**
	 * @return The list of users in the IRC channel
	 */
	public List<String> getUsers()
	{
		return _users;
	}
	
	/**
	 * Gets the IRC channel of the IRC bot
	 * @return The IRC channel with pound symbol
	 */
	public String getChannel()
	{
		return String.format("#%s", _channel.toLowerCase());
	}
	
	/**
	 * @return The number of users connected to the IRC channel
	 */
	public int getUserCount()
	{
		return (_users != null ? _users.size() : 0);
	}
}
