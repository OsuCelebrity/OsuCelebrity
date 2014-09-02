package me.reddev.OsuCelebrity;

import me.reddev.OsuCelebrity.Osu.OsuApplication;
import me.reddev.OsuCelebrity.Twitch.TwitchManager;

public class OsuCelebrity
{
	public static OsuApplication Osu;
	public void run()
	{
		TwitchManager.run();
		
		Osu = new OsuApplication();
		Osu.Start();
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
