package me.reddev.OsuCelebrity.Twitch;

import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.OsuApiUser;

import me.reddev.OsuCelebrity.Constants.Responses;
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
	 * Adds a new user to the request list
	 * @param user The user to request
	 */
	public void addRequest(OsuApiUser user)
	{
		getRequests().AddRequest(user);
		_ircBot.sendMessage(String.format(Responses.ADDED_TO_QUEUE, user.getUserName()));
		_ircBot.sendMessage(String.format(Responses.CURRENT_QUEUE, getRequests().getRequestCount()));
	}
	
	/**
	 * Notifies the stream about who the current player is
	 * @param user The username of the player being spectated
	 */
	public void notifySpectate(String user)
	{
		_ircBot.sendMessage(String.format(Responses.CURRENT_PLAYER, user));
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
