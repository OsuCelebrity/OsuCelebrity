package me.reddev.osucelebrity.twitch;

import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.Responses;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TwitchIrcBot extends ListenerAdapter<PircBotX> implements Runnable {
  private PircBotX bot;

  private String channel;
  private String username;
  private List<String> subscribers;

  private TwitchManager twitchManager;

  private Downloader downloader;

  private final TwitchIrcSettings settings;

  /**
   * Constructs a new Twitch IRC bot.
   * 
   * @param settings The Twitch Irc settings for the program
   * @param twitchManager The active TwitchManager
   * @param downloader The generated downloader
   */
  public TwitchIrcBot(TwitchIrcSettings settings, TwitchManager twitchManager, 
      Downloader downloader) {
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
                settings.getTwitchToken()).setAutoReconnect(true).addAutoJoinChannel(getChannel())
            .buildConfiguration();
    bot = new PircBotX(config);

    this.twitchManager = twitchManager;
    this.downloader = downloader;
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
    bot.sendIRC().message(getChannel(), message);
  }

  private void commandResponder(MessageEvent<PircBotX> event, String message) {
    String[] messageSplit = message.substring(1).split(" ");
    String commandName = messageSplit[0];

    if (commandName.equalsIgnoreCase("queue")) {
      // Missing supporting arguments
      if (messageSplit.length < 2) {
        sendMessage(Responses.INVALID_FORMAT_QUEUE);
        return;
      }

      OsuApiUser selectedUser;
      try {
        selectedUser = downloader.getUser(messageSplit[1], GameModes.OSU, OsuApiUser.class);
      } catch (IOException e) {
        // I don't know what the plan should be in this case, but I didn't want the error to be
        // unhandled --Tillerino

        log.warn("error getting user", e);
        selectedUser = null;
      }
      if (selectedUser == null) {
        sendMessage(String.format(Responses.INVALID_USER , messageSplit[1]));
        return;
      }

      twitchManager.addRequest(selectedUser);
    } else if (commandName.equalsIgnoreCase("next")) {
      TwitchRequest requests = twitchManager.getRequests();
      sendMessage(String.format(Responses.NEXT_IN_QUEUE, requests.getRequestedUsers().peek()
          .toString()));
    }
  }

  private void modCommandResponder(MessageEvent<PircBotX> event, String message) {

  }

  // Listeners
  // http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html

  @Override
  public void onMessage(MessageEvent<PircBotX> event) {
    String message = event.getMessage();
    // Search through for command calls
    if (message.startsWith(settings.getTwitchIrcCommand())) {
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

  /**
   * Gets the IRC channel of the IRC bot.
   * 
   * @return The IRC channel with pound symbol
   */
  public String getChannel() {
    return String.format("#%s", channel.toLowerCase());
  }
}
