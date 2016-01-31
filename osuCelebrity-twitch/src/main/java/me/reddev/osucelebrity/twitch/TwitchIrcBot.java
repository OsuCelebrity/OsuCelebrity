package me.reddev.osucelebrity.twitch;

import static me.reddev.osucelebrity.Commands.DOWNVOTE;
import static me.reddev.osucelebrity.Commands.FORCESKIP;
import static me.reddev.osucelebrity.Commands.FORCESPEC;
import static me.reddev.osucelebrity.Commands.NOW_PLAYING;
import static me.reddev.osucelebrity.Commands.POSITION;
import static me.reddev.osucelebrity.Commands.QUEUE;
import static me.reddev.osucelebrity.Commands.UPVOTE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.Commands;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.VoteType;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.tillerino.osuApiModel.GameModes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
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

  private final TwitchApi twitchApi;
  
  private final Osu osu;

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;

  private final Clock clock;

  PircBotX bot;

  private final List<String> subscribers = new ArrayList<String>();

  private final List<CommandHandler> handlers = new ArrayList<>();

  {
    handlers.add(this::handleQueue);
    handlers.add(this::handleVote);
    handlers.add(this::handlePosition);
    handlers.add(this::handleNowPlaying);
  }
  
  private final List<CommandHandler> modHandlers = new ArrayList<>();
  
  {
    modHandlers.add(this::handleAdvance);
    modHandlers.add(this::handleSpec);
    modHandlers.add(this::handleFixClient);
    modHandlers.add(this::handleBoost);
    modHandlers.add(this::handleTimeout);
    modHandlers.add(this::handleBannedMapsFilter);
    modHandlers.add(this::handleGameMode);
    modHandlers.add(this::handleExtend);
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
      boolean handled = false;
      
      if (twitchApi.isModerator(event.getUser().getNick())) {
        for (CommandHandler commandHandler : modHandlers) {
          if (commandHandler.handle(event, message, event.getUser().getNick(), pm)) {
            handled = true;
            break;
          }
        }
      }
      
      if (!handled) {
        for (CommandHandler commandHandler : handlers) {
          if (commandHandler.handle(event, message, event.getUser().getNick(), pm)) {
            break;
          }
        }
      }
    } catch (Exception e) {
      Consumer<String> messager =
          x -> event.getChannel().send()
              .message(String.format("@%s: %s", event.getUser().getNick(), x));
      UserException.handleException(log, e, messager);
    } finally {
      pm.close();
    }
  }

  boolean handleQueue(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    // Detects queueing commands.
    String targetUser = Commands.detect(message, QUEUE);

    // Splits command options by ":"
    if (targetUser == null) {
      return false;
    } else {
      // Permits: !spec username : reason
      // Example: !spec Tillerino: for awesomeness Keepo
      targetUser = targetUser.split(":")[0].trim();
    }

    OsuUser requestedUser = getUserOrThrow(pm, targetUser);
    QueuedPlayer queueRequest =
        new QueuedPlayer(requestedUser, QueueSource.TWITCH, clock.getTime());
    spectator.performEnqueue(pm, queueRequest, "twitch:" + twitchUserName, log, event::respond);
    return true;
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
    if (!StringUtils.startsWithIgnoreCase(message, FORCESKIP)) {
      return false;
    }
    message = message.substring(FORCESKIP.length());
    
    if (spectator.advanceConditional(pm, message)) {
      sendMessage(String.format(TwitchResponses.SKIPPED_FORCE, message, event.getUser()
          .getNick()));
    }
    return true;
  }
  
  boolean handleSpec(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, FORCESPEC)) {
      return false;
    }
    message = message.substring(FORCESPEC.length());
    
    OsuUser ircUser = getUserOrThrow(pm, message);
    if (spectator.promote(pm, ircUser)) {
      sendMessage(String.format(TwitchResponses.SPECTATE_FORCE,
          event.getUser().getNick(), message));
    }
    return true;
  }

  @Nonnull
  private OsuUser getUserOrThrow(PersistenceManager pm, String username) throws IOException,
      UserException {
    OsuUser ircUser = osuApi.getUser(username, pm, 0);
    if (ircUser == null) {
      throw new UserException(String.format(Responses.INVALID_USER, username));
    }
    return ircUser;
  }
  
  boolean handlePosition(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, POSITION)) {
      return false;
    }
    message = message.substring(POSITION.length());
    
    OsuUser ircUser = getUserOrThrow(pm, message);
    int position = spectator.getQueuePosition(pm, ircUser);
    if (position != -1) {
      event.getChannel().send().message(String.format(OsuResponses.POSITION, 
          ircUser.getUserName(), position));
    } else {
      event.getChannel().send().message(String.format(OsuResponses.NOT_IN_QUEUE, 
          ircUser.getUserName()));
    }
    return true;
  }
  
  boolean handleNowPlaying(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, NOW_PLAYING)) {
      return false;
    }

    QueuedPlayer player = spectator.getCurrentPlayer(pm);
    
    OsuStatus status = osu.getClientStatus();
    if (player != null && status != null && status.getType() == Type.PLAYING) {
      event.getChannel().send().message(String.format(OsuResponses.NOW_PLAYING, 
          player.getPlayer().getUserName(), status.getDetail()));
    }
    
    return true;
  }
  
  boolean handleFixClient(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!message.equalsIgnoreCase(Commands.RESTART_CLIENT)) {
      return false;
    }
    
    try {
      osu.restartClient();
    } catch (InterruptedException e) {
      // w/e
    }
    
    return true;
  }
  
  boolean handleBoost(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, Commands.BOOST)) {
      return false;
    }

    String boostedUser = message.substring(Commands.BOOST.length());

    spectator.boost(pm, getUserOrThrow(pm, boostedUser));

    event.getChannel().send()
        .message(String.format(TwitchResponses.BOOST_QUEUE, boostedUser, event.getUser()));

    return true;
  }
  
  boolean handleTimeout(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, Commands.TIMEOUT)) {
      return false;
    }
        
    String[] split = message.substring(Commands.TIMEOUT.length()).split(" ", 2);
    
    int minutes = Math.max(0, Integer.parseInt(split[0]));
    OsuUser timeoutUser = getUserOrThrow(pm, split[1]);
    
    timeoutUser.setTimeOutUntil(clock.getTime() + 60L * 1000 * minutes);
    
    event.getChannel().send()
        .message(String.format(TwitchResponses.TIMEOUT, timeoutUser.getUserName(), minutes));
    
    return true;
  }
  
  boolean handleBannedMapsFilter(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, Commands.ADD_BANNED_MAPS_FILTER)) {
      return false;
    }

    message = message.substring(Commands.ADD_BANNED_MAPS_FILTER.length());

    spectator.addBannedMapFilter(pm, message);

    event.getChannel().send().message(String.format(TwitchResponses.ADDED_BANNED_MAPS_FILTER));

    return true;
  }
  
  boolean handleGameMode(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    if (!StringUtils.startsWithIgnoreCase(message, Commands.GAME_MODE)) {
      return false;
    }

    String[] split = message.substring(Commands.GAME_MODE.length()).split(" ", 2);

    OsuUser user = getUserOrThrow(pm, split[1]);
    
    if (split[0].equalsIgnoreCase("osu")) {
      user.setGameMode(GameModes.OSU);
    } else if (split[0].equalsIgnoreCase("taiko")) {
      user.setGameMode(GameModes.TAIKO);
    } else if (split[0].equalsIgnoreCase("ctb")) {
      user.setGameMode(GameModes.CTB);
    } else if (split[0].equalsIgnoreCase("mania")) {
      user.setGameMode(GameModes.MANIA);
    } else {
      return false;
    }
    
    event.getChannel().send().message(Responses.GAME_MODE_CHANGED);
    
    return true;
  }
  
  boolean handleExtend(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    String targetUser = Commands.detect(message, Commands.EXTEND);
    if (targetUser == null) {
      return false;
    }

    spectator.extendConditional(pm, targetUser);
    
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
