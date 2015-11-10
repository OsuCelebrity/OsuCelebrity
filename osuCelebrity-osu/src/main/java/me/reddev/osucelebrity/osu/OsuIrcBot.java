package me.reddev.osucelebrity.osu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.EnqueueResult;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osuapi.OsuApi;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.tillerino.osuApiModel.GameModes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuIrcBot extends ListenerAdapter<PircBotX> implements Runnable {
  @FunctionalInterface
  interface CommandHandler {
    boolean handle(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
        PersistenceManager pm) throws UserException, IOException;
  }

  /*
   * Injected
   */
  private final Osu osu;
  private final OsuApi osuApi;
  private final OsuIrcSettings ircSettings;
  private final PersistenceManagerFactory pmf;
  private final Spectator spectator;
  private final Clock clock;

  private PircBotX bot;

  private final List<CommandHandler> handlers = new ArrayList<>();

  {
    handlers.add(this::handleQueue);
    handlers.add(this::handleSelfQueue);
    handlers.add(this::handleSkip);
  }

  /**
   * Starts the bot.
   */
  public void run() {
    try {
      Configuration<PircBotX> config =
          new Configuration.Builder<PircBotX>()
              .setName(ircSettings.getOsuIrcUsername())
              .setLogin(ircSettings.getOsuIrcUsername())
              .addListener(this)
              .setServer(ircSettings.getOsuIrcHost(), ircSettings.getOsuIrcPort(),
                  ircSettings.getOsuIrcPassword()).setAutoReconnect(true)
              .addAutoJoinChannel(ircSettings.getOsuIrcAutoJoin()).buildConfiguration();
      bot = new PircBotX(config);

      bot.startBot();
    } catch (Exception e) {
      log.error("Exception", e);
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
  public void notifyStartingPlayer(OsuUser user) {
    bot.sendIRC().message(user.getUserName().replace(' ', '_'), OsuResponses.STARTING_SESSION);
  }

  // Listeners
  // http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html

  @Override
  public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
    log.debug("connected");
  }

  @Override
  public void onDisconnect(DisconnectEvent<PircBotX> event) throws Exception {
    log.debug("disconnected");
  }

  @Override
  public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
    log.debug("received message from {}: {}", event.getUser().getNick(), event.getMessage());
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      if (!event.getMessage().startsWith(ircSettings.getOsuIrcCommand())) {
        return;
      }

      OsuUser user = osuApi.getUser(event.getUser().getNick(), GameModes.OSU, pm, 60 * 60 * 1000L);

      String message = event.getMessage().substring(ircSettings.getOsuIrcCommand().length());

      for (CommandHandler commandHandler : handlers) {
        if (commandHandler.handle(event, message, user, pm)) {
          break;
        }
      }
    } catch (Exception e) {
      handleException(e, event.getUser());
    } finally {
      pm.close();
    }
  }

  boolean handleQueue(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws IOException, UserException {
    String[] messageSplit = message.split("\\s+", 2);
    if (!messageSplit[0].equalsIgnoreCase("queue") || messageSplit.length < 2) {
      return false;
    }

    OsuUser requestedUser = osuApi.getUser(messageSplit[1], GameModes.OSU, pm, 60 * 60 * 1000);
    if (requestedUser == null) {
      log.debug("requested non-existing user {}", messageSplit[1]);
      return true;
    }
    QueuedPlayer queueRequest = new QueuedPlayer(requestedUser, QueueSource.OSU, clock.getTime());
    EnqueueResult result = spectator.enqueue(pm, queueRequest);
    if (result == EnqueueResult.SUCCESS) {
      event.getUser().send()
          .message(String.format(Responses.QUEUE_SUCCESSFUL, requestedUser.getUserName()));
    } else if (result == EnqueueResult.FAILURE) {
      event.getUser().send()
          .message(String.format(Responses.QUEUE_UNSUCCESSFUL, requestedUser.getUserName()));
    }
    return true;
  }

  boolean handleSelfQueue(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) {
    if (!message.equalsIgnoreCase("queue")) {
      return false;
    }
    QueuedPlayer queueRequest = new QueuedPlayer(user, QueueSource.OSU, clock.getTime());
    queueRequest.setNotify(true);
    EnqueueResult result = spectator.enqueue(pm, queueRequest);
    if (result == EnqueueResult.SUCCESS) {
      event.getUser().send().message(Responses.SELF_QUEUE_SUCCESSFUL);
    } else if (result == EnqueueResult.FAILURE) {
      event.getUser().send().message(Responses.SELF_QUEUE_UNSUCCESSFU);
    }
    return true;
  }

  boolean handleSkip(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException {
    if (!message.equalsIgnoreCase("skip")) {
      return false;
    }
    if (!user.getPriviledge().canSkip) {
      throw new UserException("Unauthorized to skip.");
    }
    if (spectator.advance(pm)) {
      event.getUser().send().message("Skipped.");
    } else {
      event.getUser().send().message("Not skipped.");
    }
    return true;
  }

  OsuUser getOsuUser(GenericMessageEvent<PircBotX> event, PersistenceManager pm)
      throws IOException, UserException {
    final OsuUser user =
        osuApi.getUser(event.getUser().getNick(), GameModes.OSU, pm, 60 * 60 * 1000);
    if (user == null) {
      throw new UserException(String.format(Responses.INVALID_USER, event.getUser().getNick()));
    }
    return user;
  }

  void handleException(Exception ex, User user) {
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
    if (event.getUser().getLogin().equalsIgnoreCase(ircSettings.getOsuIrcUsername())) {
      log.info(String.format("Joined %s", event.getChannel().getName()));
    }
  }

  // End Listeners
}
