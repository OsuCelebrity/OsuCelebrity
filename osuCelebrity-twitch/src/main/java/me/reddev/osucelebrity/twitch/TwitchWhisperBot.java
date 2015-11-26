package me.reddev.osucelebrity.twitch;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osuapi.OsuApi;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchWhisperBot extends ListenerAdapter<PircBotX>
    implements Runnable {

  private class GroupServerResponse {
    private String cluster;
    private String[] servers;
    private String[] websocketsServers;
  }
   
  private static final String GROUP_SERVERS = "http://tmi.twitch.tv/servers?cluster=group";
  
  private final TwitchIrcSettings settings;
  
  private final OsuApi osuApi;

  private final Twitch twitch;

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;

  private final Clock clock;

  PircBotX bot;
  
  /**
   * Downloads a document at a given url.
   * @param url The url of the document to get
   * @return The contents of the document
   * @throws IOException If the response code is not 200
   */
  public String downloadDirect(URL url) throws IOException {
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    httpCon.setRequestProperty("Accept-Encoding", "gzip");
    httpCon.setConnectTimeout(5000);
    httpCon.setReadTimeout(5000);

    try {
      if (httpCon.getResponseCode() != 200) {
        throw new IOException("response code " + httpCon.getResponseCode());
      }

      String contentEncoding = httpCon.getContentEncoding();
      InputStream inputStream = httpCon.getInputStream();
      try {
        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
          inputStream = new GZIPInputStream(inputStream);
        }
  
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int len; (len = inputStream.read(buf)) > 0;) {
          baos.write(buf, 0, len);
        }
  
        return baos.toString("UTF-8");
      } finally {
        inputStream.close();
      }
    } finally {
      httpCon.disconnect();
    }
  }
  
  /**
   * Gets the IRC host for the group server.
   * @return The URL of the group server host
   * @throws IOException If the host cannot be found
   */
  public String getGroupServerHost() throws IOException {
    String content;
    content = downloadDirect(new URL(GROUP_SERVERS));
    
    GroupServerResponse res = new Gson().fromJson(content, GroupServerResponse.class);
    String server = res.servers[0];
    return server.substring(0, server.indexOf(":"));
  }
  
  /**
   * Sends a message to the current IRC channel.
   * 
   * @param message The message to send to the channel
   */
  public void sendMessage(String message, String username) {
    bot.sendRaw().rawLineNow(String.format("PRIVMSG #jtv :/w %s %s", username, message));
  }
  
  @Override
  public void run() {
    try {
      Configuration<PircBotX> config =
          new Configuration.Builder<PircBotX>()
              .setName(settings.getTwitchIrcUsername())
              .setLogin(settings.getTwitchIrcUsername())
              .addListener(this)
              .setServer(getGroupServerHost(), 6667,
                  settings.getTwitchToken()).setAutoReconnect(true)
              .addAutoJoinChannel("#jtv").buildConfiguration();
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
  
  //Listeners
  // http://site.pircbotx.googlecode.com/hg-history/2.0.1/apidocs/index.html

  @Override
  public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
    log.debug("whisper connected");
    //Enables whispers to be received
    event.getBot().sendRaw().rawLineNow("CAP REQ :twitch.tv/tags");
  }
 
  @Override
  public void onDisconnect(DisconnectEvent<PircBotX> event) throws Exception {
    log.debug("whisper disconnected");
  }
}