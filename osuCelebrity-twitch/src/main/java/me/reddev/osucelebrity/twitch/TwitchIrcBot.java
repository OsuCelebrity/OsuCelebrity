package me.reddev.osucelebrity.twitch;

import static me.reddev.osucelebrity.Commands.DOWNVOTE;
import static me.reddev.osucelebrity.Commands.UPVOTE;
import static me.reddev.osucelebrity.core.QQueuedPlayer.queuedPlayer;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.Commands;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.QueueVote;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.Trust;
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
import org.tillerino.osuApiModel.GameModes;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
  
  private final Trust trust;
  
  private final Twitch twitch;

  PircBotX bot;

  private final List<String> subscribers = new ArrayList<String>();

  private final List<CommandHandler> handlers = new ArrayList<>();

  {
    handlers.add(createHandler(false, this::handleQueue, Commands.QUEUE));
    handlers.add(this::handleVote);
    handlers.add(createHandler(false, this::handlePosition, Commands.POSITION));
    handlers.add(createHandler(false, this::handleNowPlaying, Commands.NOW_PLAYING));
    handlers.add(createHandler(false, this::handleReplaySpecific, Commands.REPLAY));
    handlers.add(createHandler(false, this::handleReplayCurrent, Commands.REPLAY_CURRENT));
    handlers.add(createHandler(false, this::handleLink, Commands.START_LINK));
    
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
      createBot();

      bot.startBot();
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }

  void createBot() {
    Configuration<PircBotX> config =
        new Configuration.Builder<PircBotX>()
            .setName(settings.getTwitchIrcUsername())
            .setLogin(settings.getTwitchIrcUsername())
            .addListener(this)
            .setServer(settings.getTwitchIrcHost(), settings.getTwitchIrcPort(),
                settings.getTwitchToken()).setAutoReconnect(true)
            .addAutoJoinChannel(settings.getTwitchIrcChannel()).buildConfiguration();
    bot = new PircBotX(config);
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
    // Search through for command calls
    if (!message.startsWith(settings.getTwitchIrcCommand())) {
      log.debug("Received Twitch message from {}: {}", event.getUser().getNick(), message);
      return;
    }

    message = message.substring(settings.getTwitchIrcCommand().length());

    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      for (CommandHandler commandHandler : handlers) {
        if (commandHandler.handle(event, message, event.getUser().getNick(), pm)) {
          return;
        }
      }
      log.debug("Received unknown twitch command from {}: {}", event.getUser().getNick(), message);
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
    checkTrust(pm, requesterNick);
    
    // Permits: !spec username : reason
    // Example: !spec Tillerino: for awesomeness Keepo
    targetUser = targetUser.split(":")[0].trim();

    OsuUser requestedUser = getUserOrThrow(pm, targetUser);
    QueuedPlayer queueRequest =
        new QueuedPlayer(requestedUser, QueueSource.TWITCH, clock.getTime());
    spectator.performEnqueue(pm, queueRequest, QueueVote.TWITCH + requesterNick, log,
        event::respond, msg -> whisperBot.whisper(requesterNick, msg));
  }

  void checkTrust(PersistenceManager pm, String subject) throws IOException, UserException {
    trust.checkTrust(pm, twitch.getUser(pm, subject, 60 * 1000L));
  }

  boolean handleVote(MessageEvent<PircBotX> event, String message, String twitchUserName,
      PersistenceManager pm) throws UserException, IOException {
    VoteType type = null;
    if (Commands.detect(message, UPVOTE) != null) {
      type = VoteType.UP;
    }
    if (Commands.detect(message, DOWNVOTE) != null) {
      type = VoteType.DOWN;
    }
    if (type == null) {
      return false;
    }
    log.debug("{} votes {}", twitchUserName, type);

    checkTrust(pm, twitchUserName);

    spectator.vote(pm, twitchUserName, type, event.getMessage());
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
      String formatted =
          String.format(OsuResponses.NOW_PLAYING, player.getPlayer().getUserName(),
                  player.getPlayer().getUserId(), status.getDetail());
      Integer beatmapId = osu.getBeatmapId(status.getDetail());
      if (beatmapId != null) {
        formatted += " https://osu.ppy.sh/b/" + beatmapId; 
      }
      event.getChannel().send().message(formatted);
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
  
  void handleLink(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    TwitchUser user = twitch.getUser(pm, twitchUserName, 0L);
    
    if (user.getOsuUser() != null) {
      throw new UserException("Your account is already linked.");
    }
    
    user.setLinkString(UUID.randomUUID().toString());
    
    whisperBot.whisper(twitchUserName,
        String.format(TwitchResponses.LINK_INSTRUCTIONS, user.getLinkString()));
  }

  void handleReplayCurrent(MessageEvent<PircBotX> event, String message,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
    if (currentPlayer == null) {
      return;
    }
    
    URL replayLink = twitchApi.getReplayLink(currentPlayer);
    if (replayLink == null) {
      throw new UserException(String.format(Responses.REPLAY_NOT_FOUND, currentPlayer.getPlayer()
          .getUserName()));
    }
    
    event
        .getChannel()
        .send()
        .message(
            String.format(TwitchResponses.REPLAY, currentPlayer.getPlayer().getUserName(),
                replayLink.toString()));
  }

  void handleReplaySpecific(MessageEvent<PircBotX> event, String targetPlayer,
      String twitchUserName, PersistenceManager pm) throws UserException, IOException {
    QueuedPlayer player;
    try (JDOQuery<QueuedPlayer> query = new JDOQuery<>(pm).select(queuedPlayer).from(queuedPlayer)) {
      player =
          query
              .where(
                  queuedPlayer.player.userName.toLowerCase().eq(targetPlayer.trim().toLowerCase()),
                  queuedPlayer.state.loe(QueuedPlayer.SPECTATING)).orderBy(queuedPlayer.id.desc())
              .fetchFirst();
    }
    
    if (player == null) {
      throw new UserException(String.format(Responses.REPLAY_NOT_FOUND, targetPlayer));
    }
    
    URL replayLink = twitchApi.getReplayLink(player);
    if (replayLink == null) {
      throw new UserException(String.format(Responses.REPLAY_NOT_FOUND, player.getPlayer().getUserName()));
    }
    
    event
        .getChannel()
        .send()
        .message(
            String.format(TwitchResponses.REPLAY, player.getPlayer().getUserName(),
                replayLink.toString()));
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
      log.debug("{} invokes {}{}", twitchUserName, settings.getTwitchIrcCommand(), message);
      handler.handle(event, remainingMessage, twitchUserName, pm);
      return true;
    };
  }
}
