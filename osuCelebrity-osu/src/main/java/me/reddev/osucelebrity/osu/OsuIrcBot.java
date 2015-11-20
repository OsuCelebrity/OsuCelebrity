package me.reddev.osucelebrity.osu;

import com.google.common.collect.ImmutableList;

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
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Configuration;
import org.pircbotx.Configuration.Builder;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

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

  private Set<String> onlineUsers = new ConcurrentSkipListSet<>();

  private final List<CommandHandler> handlers = new ArrayList<>();

  {
    handlers.add(this::handleQueue);
    handlers.add(this::handleSelfQueue);
    handlers.add(this::handleSkip);
    handlers.add(this::handleMute);
    handlers.add(this::handleOpt);
  }

  /**
   * Starts the bot.
   */
  public void run() {
    try {
      Builder<PircBotX> configBuilder =
          new Configuration.Builder<PircBotX>()
              .setName(ircSettings.getOsuIrcUsername())
              .setLogin(ircSettings.getOsuIrcUsername())
              .addListener(this)
              .setServer(ircSettings.getOsuIrcHost(), ircSettings.getOsuIrcPort(),
                  ircSettings.getOsuIrcPassword()).setAutoReconnect(true);
      Stream.of(ircSettings.getOsuIrcAutoJoin().split(",")).forEach(
          configBuilder::addAutoJoinChannel);
      Configuration<PircBotX> config = configBuilder.buildConfiguration();
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

      OsuUser user = osuApi.getUser(event.getUser().getNick(), pm, 60 * 60 * 1000L);

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
    if (!messageSplit[0].equalsIgnoreCase("q") || messageSplit.length < 2) {
      return false;
    }

    OsuUser requestedUser = osuApi.getUser(messageSplit[1], pm, 60 * 60 * 1000);
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
    if (!message.equalsIgnoreCase("q")) {
      return false;
    }
    QueuedPlayer queueRequest = new QueuedPlayer(user, QueueSource.OSU, clock.getTime());
    EnqueueResult result = spectator.enqueue(pm, queueRequest);
    if (result == EnqueueResult.SUCCESS) {
      event.getUser().send().message(Responses.SELF_QUEUE_SUCCESSFUL);
    } else if (result == EnqueueResult.FAILURE) {
      event.getUser().send().message(Responses.SELF_QUEUE_UNSUCCESSFU);
    }
    return true;
  }

  boolean handleSkip(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, "forceskip ")) {
      return false;
    }
    if (!user.getPrivilege().canSkip) {
      throw new UserException("Unauthorized to skip.");
    }
    message = message.substring("forceskip ".length());
    OsuUser ircUser = osuApi.getUser(message, pm, 0);
    if (ircUser != null) {
      if (spectator.advanceConditional(pm, ircUser)) {
        event.getUser().send().message("Skipped.");
      } else {
        event.getUser().send().message("Not skipped.");
      }
    }
    return true;
  }
  
  boolean handleMute(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (message.equalsIgnoreCase("mute")) {
      user.setAllowsNotifications(false);
      event.getUser().send().message(String.format(OsuResponses.MUTED));
      return true;
    }
    if (message.equalsIgnoreCase("unmute")) {
      user.setAllowsNotifications(true);
      event.getUser().send().message(String.format(OsuResponses.UNMUTED));
      return true;
    }
    return false;
  }
  
  boolean handleOpt(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (message.equalsIgnoreCase("optout")) {
      user.setAllowsSpectating(false);
      spectator.removeFromQueue(pm, user);
      event.getUser().send().message(String.format(OsuResponses.OPTOUT));
      return true;
    }
    if (message.equalsIgnoreCase("optin")) {
      user.setAllowsSpectating(true);
      event.getUser().send().message(String.format(OsuResponses.OPTIN));
      return true;
    }
    return false;
  }

  OsuUser getOsuUser(GenericMessageEvent<PircBotX> event, PersistenceManager pm)
      throws IOException, UserException {
    final OsuUser user = osuApi.getUser(event.getUser().getNick(), pm, 60 * 60 * 1000);
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
    if (event.getUser().getNick().equalsIgnoreCase(ircSettings.getOsuIrcUsername())) {
      log.info(String.format("Joined %s", event.getChannel().getName()));
    }
    onlineUsers.add(event.getUser().getNick());
  }

  @Override
  public void onQuit(QuitEvent<PircBotX> event) throws Exception {
    onlineUsers.remove(event.getUser().getNick());
  }

  @Override
  public void onServerResponse(ServerResponseEvent<PircBotX> event) throws Exception {
    if (event.getCode() == 353) {
      ImmutableList<String> parsedResponse = event.getParsedResponse();

      String[] usernames = parsedResponse.get(parsedResponse.size() - 1).split(" ");

      for (int i = 0; i < usernames.length; i++) {
        String nick = usernames[i];

        if (nick.startsWith("@") || nick.startsWith("+")) {
          nick = nick.substring(1);
        }

        onlineUsers.add(nick);
      }
    } else {
      super.onServerResponse(event);
    }
  }

  Set<String> getOnlineUsers() {
    return onlineUsers;
  }

  // End Listeners
}
