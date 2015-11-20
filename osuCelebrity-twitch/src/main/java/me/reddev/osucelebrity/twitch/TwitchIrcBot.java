package me.reddev.osucelebrity.twitch;

import static me.reddev.osucelebrity.Commands.DOWNVOTE;
import static me.reddev.osucelebrity.Commands.QUEUE;
import static me.reddev.osucelebrity.Commands.UPVOTE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.EnqueueResult;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.VoteType;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchIrcBot extends ListenerAdapter<PircBotX> implements Runnable {
  @FunctionalInterface
  interface CommandHandler {
    boolean handle(MessageEvent<PircBotX> event, String message, String twitchUserName,
        PersistenceManager pm) throws UserException, IOException;
  }

  private final TwitchIrcSettings settings;

  private final OsuApi osuApi;

  private final Twitch twitch;

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;

  private final Clock clock;

  PircBotX bot;

  private final List<String> subscribers = new ArrayList<String>();

  private final List<CommandHandler> handlers = new ArrayList<>();

  {
    handlers.add(this::handleQueue);
    handlers.add(this::handleVote);
    handlers.add(this::handleAdvance);
  }

  @Override
  public void run() {
    try {
      Configuration<PircBotX> config =
          new Configuration.Builder<PircBotX>()
              .setName(settings.getTwitchIrcUsername())
              .setLogin(settings.getTwitchIrcUsername())
              .addListener(this)
              .setServer(settings.getTwitchIrcHost(), settings.getTwitchIrcPort(),
                  settings.getTwitchToken()).setAutoReconnect(true)
              .addAutoJoinChannel(settings.getTwitchIrcChannel()).buildConfiguration();
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
  public void sendMessage(String message) {
    bot.sendIRC().message(settings.getTwitchIrcChannel(), message);
  }

  // TODO make this Twitch specific
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
  public void onMessage(MessageEvent<PircBotX> event) {
    String message = event.getMessage();
    log.debug("Received Twitch message: " + message);
    // Search through for command calls
    if (!message.startsWith(settings.getTwitchIrcCommand())) {
      return;
    }

    message = message.substring(settings.getTwitchIrcCommand().length());

    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      for (CommandHandler commandHandler : handlers) {
        if (commandHandler.handle(event, message, event.getUser().getNick(), pm)) {
          break;
        }
      }
    } catch (Exception e) {
      handleException(e, event.getUser());
    } finally {
      pm.close();
    }
  }

  boolean handleQueue(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (StringUtils.startsWithIgnoreCase(message, QUEUE)) {
      String targetUser = message.substring(QUEUE.length());
      OsuUser requestedUser = osuApi.getUser(targetUser, pm, 60 * 60 * 1000L);
      if (requestedUser == null) {
        throw new UserException(String.format(TwitchResponses.INVALID_USER, targetUser));
      }
      QueuedPlayer queueRequest =
          new QueuedPlayer(requestedUser, QueueSource.TWITCH, clock.getTime());
      EnqueueResult result = spectator.enqueue(pm, queueRequest);
      if (result == EnqueueResult.SUCCESS) {
        event.getChannel().send()
            .message(String.format(TwitchResponses.QUEUE_SUCCESSFUL, requestedUser.getUserName()));
      }
      return true;
    }
    return false;
  }

  boolean handleVote(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    VoteType type = null;
    if (message.equalsIgnoreCase(UPVOTE)) {
      type = VoteType.UP;
    }
    if (message.equalsIgnoreCase(DOWNVOTE)) {
      type = VoteType.DOWN;
    }
    if (type == null) {
      return false;
    }
    spectator.vote(pm, twitchUserName, type);
    return true;
  }

  boolean handleAdvance(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, "forceskip ")) {
      return false;
    }
    message = message.substring("forceskip ".length());
    if (event.getChannel().isOp(event.getUser())) {
      OsuUser ircUser = osuApi.getUser(message, pm, 0);
      if (ircUser != null) {
        if (spectator.advanceConditional(pm, ircUser)) {
          sendMessage(String.format(TwitchResponses.SKIPPED_FORCE, message, event.getUser()
              .getNick()));
        }
      }
    }
    return true;
  }

  @Override
  public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
    String message = event.getMessage();
    Pattern specialUserRegex = Pattern.compile("SPECIALUSER ([^ ]+) (subscriber|turbo)");
    Matcher matches = specialUserRegex.matcher(message);

    // Nothing important - no special messages
    if (!matches.matches()) {
      return;
    }

    if (matches.group(2).equalsIgnoreCase("subscriber")) {
      subscribers.add(matches.group(1).toLowerCase());
    } else {
      // TODO: Add a special case for turbo users
    }
  }

  @Override
  public void onJoin(JoinEvent<PircBotX> event) {
    if (event.getUser().getLogin().equalsIgnoreCase(settings.getTwitchIrcUsername())) {
      // Ask for subscription and admin information
      event.getBot().sendRaw().rawLine("CAP REQ :twitch.tv/membership");
      log.info(String.format("Joined %s", event.getChannel().getName()));
    }
  }

  // End Listeners

  /**
   * @return The list of subscribers in the IRC channel.
   */
  public List<String> getSubscribers() {
    return subscribers;
  }
}
