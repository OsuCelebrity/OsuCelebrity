package me.reddev.OsuCelebrity.Twitch;

import java.io.IOException;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Logging.Logger;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

public class IRCBot extends PircBot
{
	private String _channel, _username, _password;
	
	
	/**
	 * Constructs a new Twitch IRC bot
	 * @param channel The username of the Twitch channel to connect to
	 * @param username The username of the Twitch IRC bot
	 * @param password The IRC password of the Twitch IRC bot
	 */
	public IRCBot(String channel, String username, String password)
	{
		_channel = channel;
		_username = username;
		_password = password;
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
		
		this.joinChannel(GetChannel());
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
		sendMessage(GetChannel(), message);
	}

	/**
	 * Gets the IRC channel of the IRC bot
	 * @return The IRC channel with pound symbol
	 */
	public String GetChannel()
	{
		return String.format("#%s", _channel.toLowerCase());
	}

	// Listeners
	
	@Override
	protected void onMessage(String channel, String sender,
			String login, String hostname, String message)
	{
		//Search through for command calls
	}
	
	@Override
	protected void onPrivateMessage(String sender, String login, 
			String hostname, String message)
	{
		Logger.Info(message);
	}

	@Override
	protected void onJoin(String channel, String sender, String login,
			String hostname)
	{
		//Ask for subscription and admin information
		this.sendRawLine("TWITCHCLIENT 3");
		Logger.Info(String.format("Joined %s", channel));
	}
	
	// End Listeners
}
