package me.reddev.OsuCelebrity.Osu;

import java.io.IOException;
import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Osu.OsuIRCBot.OsuIRCBotSettings;

public class OsuApplication
{
	private OsuIRCBot _bot;
	private OsuIRCBotSettings _settings;
	
	public OsuApplication(OsuIRCBotSettings settings) {
		super();
		this._settings = settings;
	}

	/**
	 * Connects the Application to the servers
	 */
	public void start()
	{
		_bot = new OsuIRCBot(this, _settings);
		_bot.start();
	}
	
	/**
	 * Sends a message to Bancho to start spectating a given user
	 * @param osuUser The username of the Osu! player to spectate
	 * @throws IOException Unable to run spectate command
	 */
	public void spectate(String osuUser) throws IOException
	{
		Runtime rt = Runtime.getRuntime();
		String command = String.format("\"%s\" \"%s\"", _settings.getOsuPath(), String.format(Constants.OSU_COMMAND_SPECTATE, osuUser));
		rt.exec(command);
	}
}
