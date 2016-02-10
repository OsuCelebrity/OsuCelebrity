package me.reddev.osucelebrity.twitch;

import static me.reddev.osucelebrity.Commands.DOWNVOTE;
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

  @FunctionalInterface
  interface ConfirmedCommandHandler {
    void handle(MessageEvent<PircBotX> event, String arguments, String twitchUserName,
        PersistenceManager pm) throws UserException, IOException;
  }

  private final TwitchIrcSettings settings;

  private final OsuApi osuApi;

  private final TwitchApi twitchApi;
  
  private final Osu osu;

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;

  private final Clock clock;
  
  private final TwitchWhisperBot whisperBot;

  PircBotX bot;

  private final List<String> subscribers = new ArrayList<String>();

  private final List<CommandHandler> handlers = new ArrayList<>();

  {
    handlers.add(createHandler(false, this::handleQueue, Commands.QUEUE));
    handlers.add(this::handleVote);
    handlers.add(createHandler(false, this::handlePosition, Commands.POSITION));
    handlers.add(createHandler(false, this::handleNowPlaying, Commands.NOW_PLAYING));
    
    // MOD COMMANDS
    handlers.add(createHandler(true, this::handleAdvance, Commands.FORCESKIP));
    handlers.add(createHandler(true, this::handleSpec, Commands.FORCESPEC));
    handlers.add(createHandler(true, this::handleFixClient, Commands.RESTART_CLIENT));
    handlers.add(createHandler(true, this::handleBoost, Commands.BOOST));
    handlers.add(createHandler(true, this::handleTimeout, Commands.TIMEOUT));
    handlers.add(createHandler(true, this::handleBannedMapsFilter, Commands.ADD_BANNED_MAPS_FILTER));
    handlers.add(createHandler(true, this::handleGameMode, Commands.GAME_MODE));
    handlers.add(createHandler(true, this::handleExtend, Commands.EXTEND));
    handlers.add(createHandler(true, this::handleFreeze, Commands.FREEZE));
    handlers.add(createHandler(true, this::handleUnfreeze, Commands.UNFREEZE));
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
      for (CommandHandler commandHandler : handlers) {
        if (commandHandler.handle(event, message, event.getUser().getNick(), pm)) {
          break;
        }
      }
    } catch (Exception e) {
      Consumer<String> messager =
          x -> whisperBot.whisper(event.getUser().getNick(), x);
      UserException.handleException(log, e, messager);
    } finally {
      pm.close();
    }
  }

  void handleQueue(MessageEvent<PircBotX> event, String targetUser, String requesterNick,
      PersistenceManager pm) throws UserException, IOException {
    // Permits: !spec username : reason
    // Example: !spec Tillerino: for awesomeness Keepo
    targetUser = targetUser.split(":")[0].trim();

    OsuUser requestedUser = getUserOrThrow(pm, targetUser);
    QueuedPlayer queueRequest =
        new QueuedPlayer(requestedUser, QueueSource.TWITCH, clock.getTime());
    spectator.performEnqueue(pm, queueRequest, "twitch:" + requesterNick, log, event::respond,
        msg -> whisperBot.whisper(requesterNick, msg));
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
  
  void requireMod(MessageEvent<PircBotX> event) throws UserException {
    if (!twitchApi.isModerator(event.getUser().getNick())) {
      throw new UserException("You're not a mod.");
    }
  }

  void handleAdvance(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    if (spectator.advanceConditional(pm, message)) {
      sendMessage(String.format(TwitchResponses.SKIPPED_FORCE, message, event.getUser()
          .getNick()));
    }
  }
  
  void handleSpec(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    OsuUser ircUser = getUserOrThrow(pm, message);
    if (spectator.promote(pm, ircUser)) {
      sendMessage(String.format(TwitchResponses.SPECTATE_FORCE,
          event.getUser().getNick(), message));
    }
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
  
  void handlePosition(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    OsuUser ircUser = getUserOrThrow(pm, message);
    int position = spectator.getQueuePosition(pm, ircUser);
    if (position != -1) {
      event.getChannel().send().message(String.format(OsuResponses.POSITION, 
          ircUser.getUserName(), position));
    } else {
      throw new UserException(String.format(OsuResponses.NOT_IN_QUEUE, 
          ircUser.getUserName()));
    }
  }
  
  void handleNowPlaying(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    QueuedPlayer player = spectator.getCurrentPlayer(pm);
    
    OsuStatus status = osu.getClientStatus();
    if (player != null && status != null && status.getType() == Type.PLAYING) {
      event.getChannel().send().message(String.format(OsuResponses.NOW_PLAYING, 
          player.getPlayer().getUserName(), status.getDetail()));
    }
  }
  
  void handleFixClient(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    try {
      osu.restartClient();
    } catch (InterruptedException e) {
      // w/e
    }
  }
  
  void handleBoost(MessageEvent<PircBotX> event, String boostedUser, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    spectator.boost(pm, getUserOrThrow(pm, boostedUser));

    event.getChannel().send()
        .message(String.format(TwitchResponses.BOOST_QUEUE, boostedUser, event.getUser().getNick()));
  }
  
  void handleTimeout(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    String[] split = message.split(" ", 2);
    
    int minutes = Math.max(0, Integer.parseInt(split[0]));
    OsuUser timeoutUser = getUserOrThrow(pm, split[1]);
    
    timeoutUser.setTimeOutUntil(clock.getTime() + 60L * 1000 * minutes);
    
    event.getChannel().send()
        .message(String.format(TwitchResponses.TIMEOUT, timeoutUser.getUserName(), minutes));
  }
  
  void handleBannedMapsFilter(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    spectator.addBannedMapFilter(pm, message);

    event.getChannel().send().message(String.format(TwitchResponses.ADDED_BANNED_MAPS_FILTER));
  }
  
  void handleGameMode(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    String[] split = message.split(" ", 2);

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
      throw new UserException("Unknown game mode.");
    }
    
    event.getChannel().send().message(Responses.GAME_MODE_CHANGED);
  }
  
  void handleExtend(MessageEvent<PircBotX> event, String targetUser,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    spectator.extendConditional(pm, targetUser);
  }

  void handleFreeze(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    spectator.setFrozen(true);
  }

  void handleUnfreeze(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    spectator.setFrozen(false);
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
  
  CommandHandler createHandler(boolean requiresMod, ConfirmedCommandHandler handler,
      String... triggers) {
    return (event, message, twitchUserName, pm) -> {
      String remainingMessage = Commands.detect(message, triggers);
      if (remainingMessage == null) {
        return false;
      }
      if (requiresMod) {
        requireMod(event);
      }
      log.debug("{} invokes {}", twitchUserName, message);
      handler.handle(event, remainingMessage, twitchUserName, pm);
      return true;
    };
  }
}
