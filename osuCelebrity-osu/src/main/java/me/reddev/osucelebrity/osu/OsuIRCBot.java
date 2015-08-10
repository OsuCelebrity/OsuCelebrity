package me.reddev.osucelebrity.osu;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.twitch.Twitch;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiUser;

@Slf4j
public class OsuIRCBot extends ListenerAdapter<PircBotX> implements Runnable {
  private PircBotX _bot;

  private String _username;

  private final Twitch _twitch;
  private final Downloader _downloader;

  private final OsuIrcSettings _ircSettings;

  /**
   * Constructs a new Osu! IRC bot
   * 
   * @param _manager The associated Twitch manager
   * @param username The username of the Osu! IRC bot
   * @param password The IRC password of the Osu! IRC bot
   */
  public OsuIRCBot(OsuIrcSettings ircSettings, OsuApiSettings apiSettings, Twitch twitch) {
    _username = ircSettings.getOsuIrcUsername();
    _twitch = twitch;
    _downloader = new Downloader(apiSettings.getOsuApiKey());
    _ircSettings = ircSettings;

    // Reset bot
    Configuration<PircBotX> config =
        new Configuration.Builder<PircBotX>()
            .setName(_username)
            .setLogin(_username)
            .addListener(this)
            .setServer(ircSettings.getOsuIrcHost(), ircSettings.getOsuIrcPort(),
                ircSettings.getOsuIrcPassword()).setAutoReconnect(true).buildConfiguration();
    _bot = new PircBotX(config);
  }

  /**
   * Connects to IRC and sets up listeners
   */
  public void start() {
    Thread botThread = new Thread(this);
    botThread.setName("OsuIRCBot");
    (botThread).start();
  }

  public void run() {
    try {
      _bot.startBot();
    } catch (IOException e) {
      log.error("OsuIRCBot IOException: " + e.getMessage());
    } catch (IrcException e) {
      log.error("OsuIRCBot IrcException: " + e.getMessage());
    }
  }

  /**
   * Disconnects from the IRC server
   */
  public void stop() {
    if (_bot.isConnected())
      _bot.stopBotReconnect();
  }

  /**
   * Sends a message to the current IRC channel
   * 
   * @param message The message to send to the channel
   */
  public void sendMessage(GenericChannelEvent<PircBotX> event, String message) {
    event.getBot().sendIRC().message(event.getChannel().getName(), message);
  }

  public void sendCommand(String message) {
    _bot.sendIRC().message(_ircSettings.getOsuCommandUser(), message);
  }

  /**
   * Notify the next player their turn will be up soon
   * 
   * @param user The username of the next player
   */
  public void notifyNextPlayer(String user) {
    _bot.sendIRC().message(user.toLowerCase(), Responses.UPCOMING_SESSION);
  }

  // Listeners
  // http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html

  @Override
  public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
    String[] messageSplit = event.getMessage().substring(1).split(" ");
    String commandName = messageSplit[0];
    // TODO: Accept in-game requests

    if (commandName.equalsIgnoreCase("queue")) {
      OsuApiUser selectedUser;
      try {
        selectedUser =
            _downloader.getUser(event.getUser().getNick(), GameModes.OSU, OsuApiUser.class);
      } catch (IOException e) {
        // I don't know what the plan should be in this case, but I didn't want the error to be
        // unhandled --Tillerino

        log.warn("error getting user", e);
        selectedUser = null;
      }
      if (selectedUser == null) {
        event.getUser().send().message(String.format(Responses.INVALID_USER, event.getMessage()));
        return;
      }

      _twitch.addRequest(selectedUser);
    }
  }

  @Override
  public void onJoin(JoinEvent<PircBotX> event) {
    // Ask for subscription and admin information
    if (event.getUser().getLogin().equalsIgnoreCase(_username))
      log.info(String.format("Joined %s", event.getChannel().getName()));
  }

  // End Listeners
}
