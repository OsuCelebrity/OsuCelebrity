package me.reddev.osucelebrity.twitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.twitchapi.TwitchApi;

import org.apache.commons.lang3.tuple.Pair;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchWhisperBot extends ListenerAdapter<PircBotX> implements Runnable {

  private final TwitchIrcSettings settings;

  private final TwitchApi api;

  PircBotX bot;

  Entry<String, List<String>> room;

  String server;

  /**
   * Sends a message to the current IRC channel.
   * 
   * @param message The message to send to the channel
   */
  public void whisper(String username, String message) {
    String command = String.format("PRIVMSG #" + room.getKey() + " :/w %s %s", username, message);
    bot.sendRaw().rawLineNow(command);
  }

  @Override
  public void run() {
    try {
      findServer();
      
      Configuration<PircBotX> config =
          new Configuration.Builder<PircBotX>().setName(settings.getTwitchIrcUsername())
              .setLogin(settings.getTwitchIrcUsername()).addListener(this)
              .setServer(server, 6667, settings.getTwitchToken()).setAutoReconnect(false)
              .addAutoJoinChannel("#" + room.getKey()).buildConfiguration();
      bot = new PircBotX(config);

      bot.startBot();
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }

  /**
   * Find a group chat server.
   */
  public void findServer() throws IOException {
    room = Pair.<String, List<String>>of("jtv", null);
    // api.getRoomMemberships().stream().findFirst()
    // .orElseThrow(() -> new RuntimeException("No group chat."));

    // server =
    // room.getValue().stream().filter(serv -> serv.startsWith("199."))
    // .filter(serv -> serv.endsWith(":6667"))
    // .map(serv -> serv.substring(0, serv.length() - ":6667".length())).findAny()
    // .orElseThrow(() -> new RuntimeException("No group chat server."));

    server = "irc.chat.twitch.tv";
  }

  // Listeners
  // http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html

  @Override
  public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
    log.debug("whisper connected");
    // Enables whispers to be received
    event.getBot().sendRaw().rawLineNow("CAP REQ :twitch.tv/commands");
  }

  @Override
  public void onDisconnect(DisconnectEvent<PircBotX> event) throws Exception {
    log.debug("whisper disconnected");
  }

  public void onEvent(org.pircbotx.hooks.Event<PircBotX> event) throws Exception {
    super.onEvent(event);
  }
}
