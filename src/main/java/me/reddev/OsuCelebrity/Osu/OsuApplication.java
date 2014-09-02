package me.reddev.OsuCelebrity.Osu;

import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Settings;

public class OsuApplication
{
	private OsuIRCBot _bot;
	
	/**
	 * Connects the Application to the servers
	 */
	public void Start()
	{
		_bot = new OsuIRCBot(Settings.OSU_IRC_USERNAME, Settings.OSU_IRC_PASSWORD);
		_bot.Start();
	}
	
	/**
	 * Sends a message to Bancho to start spectating a given user
	 * @param osuUser The username of the Osu! player to spectate
	 */
	public void Spectate(String osuUser)
	{
		_bot.SendCommand(String.format(Constants.OSU_COMMAND_SPECTATE, osuUser));
	}
}
