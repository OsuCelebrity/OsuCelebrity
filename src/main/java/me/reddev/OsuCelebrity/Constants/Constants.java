package me.reddev.OsuCelebrity.Constants;

public class Constants
{
	//Application properties
	public static String APP_NAME = "osu!Celebrity";

	//Twitch IRC bot properties
	public static String TWITCH_IRC_HOST = "irc.twitch.tv";
	public static int TWITCH_IRC_PORT = 6667;
	public static char TWITCH_IRC_COMMAND = '!';

	//Twitch API properties
	public static String TWITCH_API_ROOT = "https://api.twitch.tv/kraken";
	public static String TWITCH_CLIENT_ID = "g7bzpqop7kpo8de2w97ean956xmjvj2";
	public static String TWITCH_CLIENT_SECRET = "2fwfyyk0ov74g6qya6gaoyqyytqeg0h";
	public static String TWITCH_ACCESS_SCOPE = 
			"user_read+channel_read+channel_editor+chat_login";
	
	//Osu! API properties
	public static String OSU_API_ROOT = "https://osu.ppy.sh/api/";
	
	//Osu! IRC properties
	public static String OSU_IRC_HOST = "cho.ppy.sh";
	public static int OSU_IRC_PORT = 6667;
	public static String OSU_COMMAND_USER = "BanchoBot";
	
	//Osu! IRC Commands properties
	public static String OSU_COMMAND_SPECTATE = "/spectate %s";
	
	public static Boolean DEBUGGING = true;
}
