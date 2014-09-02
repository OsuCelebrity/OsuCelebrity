package me.reddev.OsuCelebrity.Osu;

import java.io.IOException;
import me.reddev.OsuCelebrity.Constants.Constants;
import me.reddev.OsuCelebrity.Logging.Logger;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;

public class OsuIRCBot extends ListenerAdapter implements Runnable
{
	private PircBotX _bot;
	
	private String _username, _password;
	
	/**
	 * Constructs a new Osu! IRC bot
	 * @param username The username of the Osu! IRC bot
	 * @param password The IRC password of the Osu! IRC bot
	 */
	public OsuIRCBot(String username, String password)
	{
		_username = username;
		_password = password;
		
		//Reset bot
		Configuration config = new Configuration.Builder()
			.setName(_username)
			.setLogin(_username)
			.addListener(this)
			.setServer(Constants.OSU_IRC_HOST, Constants.OSU_IRC_PORT, _password)
			.setAutoReconnect(true)
			.buildConfiguration();
		_bot = new PircBotX(config);
	}

	/**
	 * Connects to IRC and sets up listeners
	 */
	public void Start()
	{
		Thread botThread = new Thread(this);
		botThread.setName("OsuIRCBot");
		(botThread).start();
	}
	
	public void run()
	{
		try {
			_bot.startBot();
		} catch (IOException e) {
			Logger.Fatal("OsuIRCBot IOException: "+ e.getMessage());
		} catch (IrcException e) {
			Logger.Fatal("OsuIRCBot IrcException: "+ e.getMessage());
		}
	}

	/**
	 * Disconnects from the IRC server
	 */
	public void Stop()
	{
		if (_bot.isConnected())
			_bot.stopBotReconnect();
	}

	/**
	 * Sends a message to the current IRC channel
	 * @param message The message to send to the channel
	 */
	public void SendMessage(GenericChannelEvent event, String message)
	{
		event.getBot().sendIRC().message(event.getChannel().getName(), message);
	}

	public void SendCommand(String message)
	{
		_bot.sendIRC().message(Constants.OSU_COMMAND_USER, message);
	}
	
	// Listeners
	// http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html
	
	@Override
	public void onMessage(MessageEvent event)
	{
		String message = event.getMessage();
		//TODO: Accept in-game requests
	}
	
	@Override
	public void onJoin(JoinEvent event)
	{
		//Ask for subscription and admin information
		if(event.getUser().getLogin().equalsIgnoreCase(_username))
			Logger.Info(String.format("Joined %s", event.getChannel().getName()));
	}
	
	// End Listeners
}
