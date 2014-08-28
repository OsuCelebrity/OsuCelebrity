package me.reddev.OsuCelebrity;

import java.io.IOException;

import org.jibble.pircbot.IrcException;

import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Logging.Logger;
import me.reddev.OsuCelebrity.Twitch.IRCBot;

public class OsuCelebrity
{
	private IRCBot _ircBot;

	public void run()
	{
		StartTwitchBot();
	}

	private void StartTwitchBot()
	{
		try
		{
			_ircBot = new IRCBot(Settings.IRC_CHANNEL, Settings.IRC_USERNAME,
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
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args)
	{
		OsuCelebrity mainBot = new OsuCelebrity();
		mainBot.run();
	}

}
