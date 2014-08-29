package me.reddev.OsuCelebrity;

import java.io.IOException;

import org.jibble.pircbot.IrcException;

import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Logging.Logger;
import me.reddev.OsuCelebrity.Twitch.TwitchIRCBot;
import me.reddev.OsuCelebrity.Twitch.TwitchRequest;

public class OsuCelebrity
{
	private static TwitchIRCBot _ircBot;
	private static TwitchRequest _requests;

	public void run()
	{
		_requests = new TwitchRequest();
		StartTwitchBot();
	}

	/**
	 * Begins the Twitch TV IRC connection
	 */
	private void StartTwitchBot()
	{
		try
		{
			//Create a Twitch bot
			_ircBot = new TwitchIRCBot(Settings.IRC_CHANNEL, Settings.IRC_USERNAME,
					"oauth:"+Settings.TWITCH_TOKEN);
			_ircBot.Start();
		} catch (IrcException ex)
		{
			Logger.Fatal("IRC error or login: " + ex.getMessage());
		} catch (IOException ex)
		{
			Logger.Fatal("Could not connect to the IRC server: "
					+ ex.getMessage());
		}
	}

	/**
	 * 
	 * @return The request object connected to the twitch chat
	 */
	public static TwitchRequest getRequests()
	{
		if(_requests == null)
			return (_requests = new TwitchRequest());
		else
			return _requests;
	}
	
	/**
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args)
	{
		OsuCelebrity mainBot = new OsuCelebrity();
		mainBot.run();
	}

}
