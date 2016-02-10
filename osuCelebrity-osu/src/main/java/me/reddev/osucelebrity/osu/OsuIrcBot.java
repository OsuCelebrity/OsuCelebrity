package me.reddev.osucelebrity.osu;

import static me.reddev.osucelebrity.Commands.FORCESKIP;
import static me.reddev.osucelebrity.Commands.FORCESPEC;
import static me.reddev.osucelebrity.Commands.GAME_MODE;
import static me.reddev.osucelebrity.Commands.MUTE;
import static me.reddev.osucelebrity.Commands.OPTIN;
import static me.reddev.osucelebrity.Commands.OPTOUT;
import static me.reddev.osucelebrity.Commands.POSITION;
import static me.reddev.osucelebrity.Commands.QUEUE;
import static me.reddev.osucelebrity.Commands.SELFPOSITION;
import static me.reddev.osucelebrity.Commands.SELFQUEUE;
import static me.reddev.osucelebrity.Commands.UNMUTE;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.Commands;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.Privilege;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.EnqueueResult;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osu.Osu.PollStatusConsumer;
import me.reddev.osucelebrity.osu.PlayerStatus.PlayerStatusType;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Configuration;
import org.pircbotx.Configuration.Builder;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.events.UnknownEvent;
import org.tillerino.osuApiModel.GameModes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        PersistenceManager pm) throws UserException, IOException, InterruptedException;
  }

  @FunctionalInterface
  interface ConfirmedCommandHandler {
    void handle(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
        PersistenceManager pm) throws UserException, IOException, InterruptedException;
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
  
  private final Pinger pinger;

  PircBotX bot;

  private Set<String> onlineUsers = new ConcurrentSkipListSet<>();

  final List<CommandHandler> handlers = new ArrayList<>();
  
  Map<Integer, BlockingQueue<PollStatusConsumer>> statusConsumers = new ConcurrentHashMap<>();

  {
    handlers.add(createHandler(this::handlePosition, POSITION));
    handlers.add(createHandler(this::handleSelfPosition, SELFPOSITION));
    handlers.add(createHandler(this::handleQueue, QUEUE));
    handlers.add(createHandler(this::handleSelfQueue, SELFQUEUE));
    handlers.add(createHandler(this::handleSkip, FORCESKIP));
    handlers.add(this::handleMute);
    handlers.add(this::handleOpt);
    handlers.add(createHandler(this::handleSpec, FORCESPEC));
    handlers.add(createHandler(this::handleGameMode, GAME_MODE));
    handlers.add(createHandler(this::handleRestartClient, Commands.RESTART_CLIENT));
    handlers.add(createHandler(this::handleMod, Commands.MOD));
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
                  ircSettings.getOsuIrcPassword()).setAutoReconnect(true).setMessageDelay(500);
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
   * Notify the next player their turn will be up soon.
   * 
   * @param user The username of the next player
   */
  public void messagePlayer(OsuUser user, String message) {
    if (!ircSettings.isOsuIrcSilenced()) {
      if (synchronizeThroughPinger(() -> bot.sendIRC().message(
          user.getUserName().replace(' ', '_'), message))) {
        log.debug("MESSAGED {}: {}", user.getUserName(), message);
      }
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
  public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
    log.debug("received message from {}: {}", event.getUser().getNick(), event.getMessage());

    if (event.getUser().getNick().equals(ircSettings.getOsuCommandUser())) {
      handleBanchoBotResponse(event.getMessage());
      return;
    }
    
    if (!event.getMessage().startsWith(ircSettings.getOsuIrcCommand())) {
      return;
    }
    PersistenceManager pm = pmf.getPersistenceManager();
    try {

      OsuIrcUser ircUser = osuApi.getIrcUser(event.getUser().getNick(), pm, 0);
      if (ircUser == null || ircUser.getUser() == null) {
        throw new UserException("unrecognized user name");
      }
      
      String message = event.getMessage().substring(ircSettings.getOsuIrcCommand().length());

      for (CommandHandler commandHandler : handlers) {
        if (commandHandler.handle(event, message, ircUser.getUser(), pm)) {
          break;
        }
      }
    } catch (Exception e) {
      UserException.handleException(log, e, message -> respond(event, message));
    } finally {
      pm.close();
    }
  }

  static Pattern statusPattern = Pattern.compile("Stats for \\(.*\\)\\[https://osu.ppy.sh/u/(\\d+)\\](?: is (Idle|Watching|Modding|Playing|Afk|Multiplaying))?:");
  
  @SuppressFBWarnings("TQ")
  Optional<PlayerStatus> parseStatus(PersistenceManager pm, String message) throws IOException {
    Matcher matcher = statusPattern.matcher(message);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    int userId = Integer.parseInt(matcher.group(1));
    String typeString = matcher.group(2);
    PlayerStatusType type =
        typeString == null ? PlayerStatusType.OFFLINE : PlayerStatusType.valueOf(typeString
            .toUpperCase());
    // measure time early in case the api blocks
    long time = clock.getTime();
    OsuUser user = osuApi.getUser(userId, pm, 0);
    if (user == null) {
      return Optional.empty();
    }
    return Optional.of(new PlayerStatus(user, type, time));
  }
  
  void handleBanchoBotResponse(String message) {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      Optional<PlayerStatus> status = parseStatus(pm, message);
      if (!status.isPresent()) {
        return;
      }
      boolean handled = false;
      Queue<PollStatusConsumer> consumers = statusConsumers.get(status.get().getUser().getUserId());
      if (consumers != null) {
        for (PollStatusConsumer consumer; (consumer = consumers.poll()) != null; ) {
          handled = true;
          try {
            consumer.accept(pm, status.get());
          } catch (Exception e) {
            log.error("error while handling status", e);
          }
        }
      }
      if (!handled) {
        spectator.reportStatus(pm, status.get());
      }
    } catch (Exception e) {
      log.error("error while handling bancho response", e);
    } finally {
      pm.close();
    }
  }

  CommandHandler createHandler(ConfirmedCommandHandler handler,
      String... triggers) {
    return (event, message, user, pm) -> {
      String remainingMessage = Commands.detect(message, triggers);
      if (remainingMessage == null) {
        return false;
      }
      log.debug("{} invokes {}", user.getUserName(), message);
      handler.handle(event, remainingMessage, user, pm);
      return true;
    };
  }
  
  void handleQueue(PrivateMessageEvent<PircBotX> event, String queueTarget, OsuUser user,
      PersistenceManager pm) throws IOException, UserException {
    // Permits: !spec username : reason
    // Example: !spec Tillerino: for awesomeness Keepo
    queueTarget = queueTarget.split(":")[0].trim();

    OsuUser requestedUser = osuApi.getUser(queueTarget, pm, 60 * 60 * 1000);
    if (requestedUser == null) {
      throw new UserException(String.format(OsuResponses.INVALID_USER, queueTarget));
    }
    QueuedPlayer queueRequest = new QueuedPlayer(requestedUser, QueueSource.OSU, clock.getTime());
    spectator.performEnqueue(pm, queueRequest, "osu:" + user.getUserId(), log,
        msg -> respond(event, msg), msg -> respond(event, msg));
  }

  void handleSpec(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (!user.getPrivilege().canSkip) {
      throw new UserException("not allowed");
    }
    
    OsuUser target = osuApi.getUser(message, pm, 0);
    if (target != null) {
      if (spectator.promote(pm, target)) {
        respond(event, "spectating " + target.getUserName());
      }
    }
  }

  void handleSelfQueue(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws IOException {
    QueuedPlayer queueRequest = new QueuedPlayer(user, QueueSource.OSU, clock.getTime());
    EnqueueResult result = spectator.enqueue(pm, queueRequest, true, null, true);
    if (result == EnqueueResult.SUCCESS) {
      respond(event, Responses.SELF_QUEUE_SUCCESSFUL);
    } else if (result == EnqueueResult.FAILURE) {
      respond(event, Responses.SELF_QUEUE_UNSUCCESSFU);
    }
  }

  void handleSkip(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (!user.getPrivilege().canSkip) {
      throw new UserException("Unauthorized to skip.");
    }
    if (spectator.advanceConditional(pm, message)) {
      respond(event, "Skipped.");
    } else {
      respond(event, "Not skipped.");
    }
  }
  
  boolean handleMute(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (message.equalsIgnoreCase(MUTE)) {
      user.setAllowsNotifications(false);
      respond(event, String.format(OsuResponses.MUTED));
      return true;
    }
    if (message.equalsIgnoreCase(UNMUTE)) {
      user.setAllowsNotifications(true);
      respond(event, String.format(OsuResponses.UNMUTED));
      return true;
    }
    return false;
  }
  
  boolean handleOpt(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (message.equalsIgnoreCase(OPTOUT)) {
      user.setAllowsSpectating(false);
      log.debug("{} opted out. removing from queue", user.getUserName());
      spectator.removeFromQueue(pm, user);
      respond(event, String.format(OsuResponses.OPTOUT));
      return true;
    }
    if (message.equalsIgnoreCase(OPTIN)) {
      user.setAllowsSpectating(true);
      respond(event, String.format(OsuResponses.OPTIN));
      return true;
    }
    return false;
  }
  
  void handlePosition(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    message = message.substring(POSITION.length());
    OsuUser requestedUser = osuApi.getUser(message, pm, 60 * 60 * 1000);
    if (requestedUser == null) {
      return;
    }
    
    int position = spectator.getQueuePosition(pm, requestedUser);
    if (position != -1) {
      respond(event, String.format(OsuResponses.POSITION, 
          requestedUser.getUserName(), position));
    } else {
      respond(event, String.format(OsuResponses.NOT_IN_QUEUE, 
          requestedUser.getUserName()));
    }
  }
  
  void handleSelfPosition(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    int position = spectator.getQueuePosition(pm, user);
    if (position != -1) {
      respond(event, String.format(OsuResponses.POSITION, 
          user.getUserName(), position));
    } else {
      respond(event, String.format(OsuResponses.NOT_IN_QUEUE, 
          user.getUserName()));
    }
  }
  
  void handleGameMode(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException {
    if (message.equalsIgnoreCase("osu")) {
      user.setGameMode(GameModes.OSU);
    } else if (message.equalsIgnoreCase("taiko")) {
      user.setGameMode(GameModes.TAIKO);
    } else if (message.equalsIgnoreCase("ctb")) {
      user.setGameMode(GameModes.CTB);
    } else if (message.equalsIgnoreCase("mania")) {
      user.setGameMode(GameModes.MANIA);
    } else {
      throw new UserException("Unknown game mode.");
    }
    
    respond(event, Responses.GAME_MODE_CHANGED);
  }
  
  void handleRestartClient(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException, InterruptedException {
    if (!user.getPrivilege().canRestartClient) {
      throw new UserException("not allowed");
    }
    
    osu.restartClient();
  }
  
  void handleMod(PrivateMessageEvent<PircBotX> event, String message, OsuUser user,
      PersistenceManager pm) throws UserException, IOException, InterruptedException {
    if (!user.getPrivilege().canMod) {
      throw new UserException("not allowed");
    }
    
    OsuUser target = osuApi.getUser(message, pm, 0);
    if (target == null) {
      throw new UserException("not found: " + message);
    }
    target.setPrivilege(Privilege.MOD);
    
    respond(event, "modded");
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

  void pollIngameStatus(OsuUser player) {
    if (synchronizeThroughPinger(() -> bot.sendIRC().message(ircSettings.getOsuCommandUser(),
        "!stat " + player.getUserName()))) {
      log.debug("POLLED STATUS for: {}", player.getUserName());
    }
  }

  void pollIngameStatus(OsuUser player, PollStatusConsumer action) {
    statusConsumers.computeIfAbsent(player.getUserId(), x -> new LinkedBlockingQueue<>()).add(
        action);
    pollIngameStatus(player);
  }

  @Override
  public void onUnknown(UnknownEvent<PircBotX> event) throws Exception {
    pinger.handleUnknownEvent(event);
  }
  
  synchronized boolean synchronizeThroughPinger(Runnable runnable) {
    try {
      pinger.ping(bot);
      runnable.run();
      return true;
    } catch (IOException e) {
      log.warn("IOException while waiting for pong: {}", e.getMessage());
      return false;
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for pong: {}", e.getMessage());
      return false;
    }
  }
  
  void respond(PrivateMessageEvent<?> event, String response) {
    if (synchronizeThroughPinger(() -> event.respond(response))) {
      log.debug("RESPONDED to {}: {}", event.getUser().getNick(), response);
    }
  }
  
  // End Listeners
}
