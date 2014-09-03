package me.reddev.OsuCelebrity.Twitch;

import org.tillerino.osuApiModel.Downloader;

import me.reddev.OsuCelebrity.Constants.Settings;

public class TwitchManager 
{
	private TwitchIRCBot _ircBot;
	private TwitchRequest _requests;
	
	private Settings _settings;
	
	public TwitchManager(Settings settings) {
		super();
		this._settings = settings;
	}

	public void start()
	{
		_requests = new TwitchRequest();
		//Create a Twitch bot
		_ircBot = new TwitchIRCBot(_settings, this, new Downloader(_settings.getOsuApiKey()));
		_ircBot.start();
	}
	
	/**
	 * @return The request object connected to the Twitch chat
	 */
	public TwitchRequest getRequests()
	{
		if(_requests == null)
			return (_requests = new TwitchRequest());
		else
			return _requests;
	}
	
	/**
	 * @return The Twitch IRC bot connected to the Twitch chat
	 */
	public TwitchIRCBot getIRCBot()
	{
		return _ircBot;
	}
}
