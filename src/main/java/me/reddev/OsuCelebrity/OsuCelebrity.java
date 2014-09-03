package me.reddev.OsuCelebrity;

import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Osu.OsuApplication;
import me.reddev.OsuCelebrity.Twitch.TwitchManager;

public class OsuCelebrity
{
	private OsuApplication osu;
	private TwitchManager twitchManager;
	
	public void run()
	{
		Settings settings = new Settings();
		
		twitchManager = new TwitchManager(settings);
		twitchManager.start();
		
		osu = new OsuApplication(settings);
		osu.start();
	}
	
	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args)
	{
		OsuCelebrity mainBot = new OsuCelebrity();
		mainBot.run();
	}

}
