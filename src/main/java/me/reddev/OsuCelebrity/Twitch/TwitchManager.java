package me.reddev.OsuCelebrity.Twitch;

import me.reddev.OsuCelebrity.Constants.Settings;

public class TwitchManager 
{
	private static TwitchIRCBot _ircBot;
	private static TwitchRequest _requests;
	
	public static void run()
	{
		_requests = new TwitchRequest();
		//Create a Twitch bot
		_ircBot = new TwitchIRCBot(Settings.TWITCH_IRC_CHANNEL, Settings.TWITCH_IRC_USERNAME,
				"oauth:"+Settings.TWITCH_TOKEN);
		_ircBot.Start();
	}
	
	/**
	 * @return The request object connected to the Twitch chat
	 */
	public static TwitchRequest getRequests()
	{
		if(_requests == null)
			return (_requests = new TwitchRequest());
		else
			return _requests;
	}
	
	/**
	 * @return The Twitch IRC bot connected to the Twitch chat
	 */
	public static TwitchIRCBot getIRCBot()
	{
		return _ircBot;
	}
}
