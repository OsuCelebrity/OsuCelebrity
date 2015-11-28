package me.reddev.osucelebrity.twitch;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchApiImpl implements TwitchApi {
  private static final String TMI_BASE = "http://tmi.twitch.tv/";

  private static final String CHATTERS = "group/user/%s/chatters?_=%d";

  private final TwitchApiSettings settings;
  private final TwitchIrcSettings ircSettings;
  private final Clock clock;

  /**
   * Sends a request to the Twitch API server with POST queries.
   * 
   * @param uri The URL, relative to the API base
   * @param queries The POST queries
   * @return A JSON response by the server
   */
  private String postRequest(String uri, String... queries) {
    try {
      // Connects queries into POST string
      URL url = new URL(settings.getTwitchApiRoot() + uri);
      URLConnection conn = url.openConnection();

      // Add API headers
      conn.setRequestProperty("Client-ID", settings.getTwitchClientId());
      conn.setRequestProperty("Accept", "application/vnd.twitchtv.v2+json");
      conn.setRequestProperty("Authorization", "OAuth: " + settings.getTwitchToken());
      conn.setDoOutput(true);

      try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
        String urlParameters = Stream.of(queries).collect(Collectors.joining("&"));
        writer.write(urlParameters);
      }

      return readString(conn.getInputStream());
    } catch (IOException ex) {
      log.error(String.format("Twitch API raised %s", ex.getMessage()));
      return "";
    }
  }

  private String readString(InputStream is) throws IOException, UnsupportedEncodingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    for (int len = 0; (len = is.read(buf)) > 0;) {
      baos.write(buf, 0, len);
    }
    return baos.toString("UTF-8");
  }

  private String getTmiRequest(String uri) throws IOException {
    URL url = new URL(TMI_BASE + uri);
    URLConnection conn = url.openConnection();

    return readString(conn.getInputStream());
  }

  @CheckForNull
  ChannelChatters channelChatters = null;

  /**
   * updates channel chatters through chatters API. exceptions are logged.
   */
  public void updateChatters() {
    try {
      String req =
          getTmiRequest(String.format(CHATTERS, ircSettings.getTwitchIrcUsername().toLowerCase(),
              clock.getTime()));

      channelChatters = new Gson().fromJson(req, ChannelChatters.class);
    } catch (IOException e) {
      // we are expecting these regularly, so no need to put them in the error log
      log.warn("Exception while updating chatters", e);
    } catch (Exception e) {
      log.error("Exception while updating chatters", e);
    }
  }

  @Override
  public boolean isModerator(String username) {
    return getOnlineMods().contains(username.toLowerCase());
  }

  @Data
  public class ChannelChatters {
    int chatterCount;
    Chatters chatters;

    @Data
    public class Chatters {
      List<String> moderators;
      List<String> staff;
      List<String> admins;
      List<String> globalMods;
      List<String> viewers;
    }
  }

  @Override
  public List<String> getOnlineMods() {
    return channelChatters != null ? channelChatters.getChatters().getModerators() : Collections
        .emptyList();
  }
}
