package me.reddev.OsuCelebrity.Osu;

import java.io.IOException;

import org.tillerino.osuApiModel.OsuApiUser;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Constants.Settings;
import me.reddev.OsuCelebrity.Osu.OsuIRCBot.OsuIRCSettings;
import me.reddev.OsuCelebrity.Output.StreamOutput;
import me.reddev.OsuCelebrity.Output.StreamOutput.StreamOutputSettings;
import me.reddev.OsuCelebrity.Twitch.TwitchManager;
import me.reddev.OsuCelebrity.Twitch.TwitchRequest;

@Slf4j
public class OsuApplication implements Runnable
{
	public interface OsuApplicationSettings {
		int getSpectateDuration();
	}
	
	private OsuIRCBot _bot;
	private Settings _settings;
	private StreamOutput _output;
	private TwitchManager _manager;
	
	@Getter
	private Process osuProcess;
	
	public OsuApplication(Settings settings, TwitchManager twitchManager) {
		super();
		this._settings = settings;
		this._manager = twitchManager;
	}

	/**
	 * Connects the Application to the servers
	 */
	public void start()
	{
		_bot = new OsuIRCBot(_settings, _manager);
		_bot.start();
		
		_output = new StreamOutput(this, (StreamOutputSettings)_settings);
		_output.start();
		
		Thread applicationThread = new Thread(this);
		applicationThread.setName("OsuApplication");
		applicationThread.start();
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
		osuProcess = rt.exec(command);
	}

	public void run() 
	{
		while(true)
		{
			try {
				TwitchRequest requests = _manager.getRequests();
				OsuApiUser nextUser = requests.getRequestedUsers().poll();
				
				if(nextUser != null)
					spectate(nextUser.getUserName());
				
				Thread.sleep(_settings.getSpectateDuration());
			} catch (InterruptedException e) {
				log.error("application thread interrupted");
			} catch (IOException e) {
				log.error("unable to spectate user");
			}
		}
	}
}
