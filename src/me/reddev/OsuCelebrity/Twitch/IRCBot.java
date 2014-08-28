package me.reddev.OsuCelebrity.Twitch;

import java.io.IOException;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Logging.Logger;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

public class IRCBot extends PircBot
{
	private String _channel, _username, _password;

	public IRCBot(String channel, String username, String password)
	{
		_channel = channel;
		_username = username;
		_password = password;
	}

	public void Start() throws IOException, IrcException
	{
		setName(_username);
		setLogin(_username);

		this.setVerbose(Constants.DEBUGGING);
		this.connect(Constants.IRC_HOST, Constants.IRC_PORT, _password);
		
		this.joinChannel(GetChannel());
	}

	public void Stop()
	{
		if (isConnected())
			disconnect();
	}

	public void SendMessage(String message)
	{
		sendMessage(GetChannel(), message);
	}

	public String GetChannel()
	{
		return String.format("#%s", _channel.toLowerCase());
	}

	// Listeners
	
	@Override
	protected void onConnect()
	{
		
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
		Logger.Info(String.format("Joined %s", channel));
	}

	@Override
	protected void handleLine(String line)
	{
		//Logger.Warning(line);
	}
	
	// End Listeners
}
