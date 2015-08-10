package me.reddev.osucelebrity.osu;

import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.osu.commands.QueueSelfOsuCommandImpl;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OsuIrcBot extends ListenerAdapter<PircBotX> implements Runnable {
  private PircBotX bot;

  private String username;

  private final Osu osu;
  private final Downloader downloader;

  private final OsuIrcSettings ircSettings;

  final List<OsuCommandHandler> commandHandlers = new ArrayList<>();

  /**
   * Constructs a new Osu! IRC bot.
   */
  public OsuIrcBot(OsuIrcSettings ircSettings, OsuApiSettings apiSettings, Osu osu) {
    this.username = ircSettings.getOsuIrcUsername();
    this.osu = osu;
    this.downloader = new Downloader(apiSettings.getOsuApiKey());
    this.ircSettings = ircSettings;

    // Reset bot
    Configuration<PircBotX> config =
        new Configuration.Builder<PircBotX>()
            .setName(username)
            .setLogin(username)
            .addListener(this)
            .setServer(ircSettings.getOsuIrcHost(), ircSettings.getOsuIrcPort(),
                ircSettings.getOsuIrcPassword()).setAutoReconnect(true).buildConfiguration();
    bot = new PircBotX(config);
  }

  /**
   * Starts the bot.
   */
  public void run() {
    try {
      bot.startBot();
    } catch (IOException e) {
      log.error("OsuIRCBot IOException: " + e.getMessage());
    } catch (IrcException e) {
      log.error("OsuIRCBot IrcException: " + e.getMessage());
    }
  }

  /**
   * Disconnects from the IRC server.
   */
  public void stop() {
    if (bot.isConnected()) {
      bot.stopBotReconnect();
    }
  }

  /**
   * Sends a message to the current IRC channel.
   * 
   * @param message The message to send to the channel
   */
  public void sendMessage(GenericChannelEvent<PircBotX> event, String message) {
    event.getBot().sendIRC().message(event.getChannel().getName(), message);
  }

  public void sendCommand(String message) {
    bot.sendIRC().message(ircSettings.getOsuCommandUser(), message);
  }

  /**
   * Notify the next player their turn will be up soon.
   * 
   * @param user The username of the next player
   */
  public void notifyNextPlayer(String user) {
    bot.sendIRC().message(user.toLowerCase(), Responses.UPCOMING_SESSION);
  }

  // Listeners
  // http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html

  @Override
  public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
    String[] messageSplit = event.getMessage().substring(1).split(" ");
    String commandName = messageSplit[0];
    // TODO: Accept in-game requests

    final OsuApiUser selectedUser;
    try {
      selectedUser =
          downloader.getUser(event.getUser().getNick(), GameModes.OSU, OsuApiUser.class);
    } catch (IOException e) {
      // I don't know what the plan should be in this case, but I didn't want the error to be
      // unhandled --Tillerino

      log.warn("error getting user", e);
      event.getUser().send().message(String.format(Responses.INVALID_USER, event.getMessage()));
      return;
    }

    try {
      if (commandName.equalsIgnoreCase("queue")) {
        dispatchCommand(new QueueSelfOsuCommandImpl(osu, selectedUser));
      }
    } catch (Exception e) {
      handlException(e, event.getUser());
    }
  }

  void dispatchCommand(OsuCommand command) throws Exception {
    for (OsuCommandHandler osuCommandHandler : commandHandlers) {
      if (osuCommandHandler.handle(command)) {
        return;
      }
    }
  }

  void handlException(Exception ex, User user) {
    try {
      throw ex;
    } catch (UserException e) {
      // no need to log this
      user.send().message(e.getMessage());
    } catch (IOException e) {
      log.error("external error", e);
      user.send().message("external error");
    } catch (Exception e) {
      log.error("internal error", e);
      user.send().message("internal error");
    }
  }

  @Override
  public void onJoin(JoinEvent<PircBotX> event) {
    // Ask for subscription and admin information
    if (event.getUser().getLogin().equalsIgnoreCase(username)) {
      log.info(String.format("Joined %s", event.getChannel().getName()));
    }
  }

  // End Listeners
}
