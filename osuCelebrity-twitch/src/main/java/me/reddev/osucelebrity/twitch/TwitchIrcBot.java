package me.reddev.osucelebrity.twitch;

import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.CommandDispatcher;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitch.commands.NextUserTwitchCommandImpl;
import me.reddev.osucelebrity.twitch.commands.QueueUserTwitchCommandImpl;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
public class TwitchIrcBot extends ListenerAdapter<PircBotX> implements Runnable {
  PircBotX bot;

  private String channel;
  private String username;
  private List<String> subscribers;

  private Twitch twitch;

  private OsuApi osuApi;

  private final TwitchIrcSettings settings;
  
  private final PersistenceManagerFactory pmf;
  
  final CommandDispatcher<TwitchCommand> dispatcher = 
      new CommandDispatcher<TwitchCommand>();

  /**
   * Constructs a new Twitch IRC bot.
   * 
   * @param settings The Twitch Irc settings for the program
   * @param twitch The active TwitchManager
   * @param osuApi The generated downloader
   */
  public TwitchIrcBot(TwitchIrcSettings settings, OsuApi osuApi, Twitch twitch,
      PersistenceManagerFactory pmf) {
    this.settings = settings;

    this.channel = settings.getTwitchIrcChannel();
    this.username = settings.getTwitchIrcUsername();

    // Reset user lists
    this.subscribers = new ArrayList<String>();

    // Reset bot
    Configuration<PircBotX> config =
        new Configuration.Builder<PircBotX>()
            .setName(this.username)
            .setLogin(this.username)
            .addListener(this)
            .setServer(settings.getTwitchIrcHost(), settings.getTwitchIrcPort(),
                settings.getTwitchToken()).setAutoReconnect(true)
                .addAutoJoinChannel(settings.getTwitchIrcChannel())
            .buildConfiguration();
    bot = new PircBotX(config);

    this.twitch = twitch;
    this.osuApi = osuApi;
    this.pmf = pmf;
  }

  /**
   * Connects to IRC and sets up listeners.
   */
  public void start() {
    Thread botThread = new Thread(this);
    botThread.setName("TwitchIRCBot");
    (botThread).start();
  }

  @Override
  public void run() {
    try {
      bot.startBot();
    } catch (IOException e) {
      log.error("TwitchIRCBot IOException: " + e.getMessage());
    } catch (IrcException e) {
      log.error("TwitchIRCBot IrcException: " + e.getMessage());
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
  
  /**
   * Acts on a given command message.
   * @param event The irc event for the command.
   * @param message The message (without the command string ie. !)
   */
  private void commandResponder(MessageEvent<PircBotX> event, String message) {
    String[] messageSplit = message.split(" ");
    String commandName = messageSplit[0];
    String user = event.getUser().getNick();

    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      if (commandName.equalsIgnoreCase("queue")) {
        // Missing supporting arguments
        if (messageSplit.length < 2) {
          sendMessage(TwitchResponses.INVALID_FORMAT_QUEUE);
          return;
        }
  
        OsuUser selectedUser;
        try {
          selectedUser = osuApi.getUser(messageSplit[1], GameModes.OSU, pm, 10 * 60 * 1000);
        } catch (IOException e) {
          log.warn("error getting user", e);
          selectedUser = null;
        }
        
        //User gave an invalid username
        if (selectedUser == null) {
          sendMessage(String.format(TwitchResponses.INVALID_USER , messageSplit[1]));
          return;
        }
        
        dispatcher.dispatchCommand(new QueueUserTwitchCommandImpl(twitch, user, selectedUser, pm));
      } else if (commandName.equalsIgnoreCase("next")) {
        dispatcher.dispatchCommand(new NextUserTwitchCommandImpl(twitch, user, pm));
      }
    } catch (Exception e) {
      handleException(e, event.getUser());
    } finally {
      pm.close();
    }
  }

  private void modCommandResponder(MessageEvent<PircBotX> event, String message) {

  }
  
  //TODO make this Twitch specific
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
    if (message.startsWith(settings.getTwitchIrcCommand())) {
      message = message.substring(settings.getTwitchIrcCommand().length());
      
      if (event.getChannel().isOp(event.getUser())) {
        modCommandResponder(event, message);
      }
      commandResponder(event, message);
    }
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
    if (event.getUser().getLogin().equalsIgnoreCase(username)) {
      // Ask for subscription and admin information
      event.getBot().sendRaw().rawLine("TWITCHCLIENT 3");
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
