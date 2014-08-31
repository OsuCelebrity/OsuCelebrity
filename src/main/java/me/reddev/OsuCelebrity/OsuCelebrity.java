package me.reddev.OsuCelebrity;

import me.reddev.OsuCelebrity.Twitch.TwitchManager;

public class OsuCelebrity
{
	public void run()
	{
		TwitchManager.run();
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
