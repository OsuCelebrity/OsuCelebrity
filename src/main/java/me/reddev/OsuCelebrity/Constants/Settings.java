package me.reddev.OsuCelebrity.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.Data;
import me.reddev.OsuCelebrity.Osu.OsuIRCBot.OsuIRCBotSettings;
import me.reddev.OsuCelebrity.Twitch.TwitchAPI.TwitchApiSettings;
import me.reddev.OsuCelebrity.Twitch.TwitchIRCBot.TwitchIrcSettings;

@Data
public class Settings implements OsuIRCBotSettings, TwitchIrcSettings, TwitchApiSettings
{
	//Twitch IRC settings
	private final String twitchIrcChannel;
	private final String twitchIrcUsername;
	private final String twitchToken;

	//Osu! API settings
	private final String osuApiKey;

	//Osu! Account Settings
	private final String osuIrcUsername;
	private final String osuIrcPassword;
	private final String twitchClientId;
	private final String twitchClientSecret;

	public Settings(Properties properties) {
		if((twitchIrcChannel = properties.getProperty("twitchIrcChannel")) == null) {
			throw new RuntimeException("please supply the parameter twitchIrcChannel");
		}
		if((twitchIrcUsername = properties.getProperty("twitchIrcUsername")) == null) {
			throw new RuntimeException("please supply the parameter twitchIrcUsername");
		}
		if((twitchToken = properties.getProperty("twitchToken")) == null) {
			throw new RuntimeException("please supply the parameter twitchToken");
		}
		if((osuApiKey = properties.getProperty("osuApiKey")) == null) {
			throw new RuntimeException("please supply the parameter osuApiKey");
		}
		if((osuIrcUsername = properties.getProperty("osuIrcUsername")) == null) {
			throw new RuntimeException("please supply the parameter osuIrcUsername");
		}
		if((osuIrcPassword = properties.getProperty("osuIrcPassword")) == null) {
			throw new RuntimeException("please supply the parameter osuIrcPassword");
		}
		if((twitchClientId = properties.getProperty("twitchClientId")) == null) {
			throw new RuntimeException("please supply the parameter twitchClientId");
		}
		if((twitchClientSecret = properties.getProperty("twitchClientSecret")) == null) {
			throw new RuntimeException("please supply the parameter twitchClientSecret");
		}
	}

	public Settings() {
		this(getProperties("osuCelebrity.properties"));
	}

	public static Properties getProperties(String resourceName) {
		InputStream is = Settings.class.getClassLoader().getResourceAsStream(resourceName);
		if(is == null) {
			throw new RuntimeException("resource " + resourceName + " not found");
		}

		Properties properties = new Properties();
		try {
			properties.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// suppress this
				e.printStackTrace();
			}
		}
		return properties;
	}
}
