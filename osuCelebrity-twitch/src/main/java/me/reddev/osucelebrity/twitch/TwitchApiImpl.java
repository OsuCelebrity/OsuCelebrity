package me.reddev.osucelebrity.twitch;

import com.google.gson.Gson;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchApiImpl implements TwitchApi {
  private static final String TMI_BASE = "http://tmi.twitch.tv/";
  
  private static final String CHATTERS = "group/user/%s/chatters?_=%d";
  
  private final TwitchApiSettings settings;
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

      OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");

      String urlParameters = join(queries, "&");
      
      writer.write(urlParameters);
      writer.flush();

      String line;
      String output = "";
      BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), 
          "UTF-8"));

      while ((line = reader.readLine()) != null) {
        // Concatenate read lines
        output = output.concat(line);
      }
      writer.close();
      reader.close();

      return output;
    } catch (IOException ex) {
      log.error(String.format("Twitch API raised %s", ex.getMessage()));
      return "";
    }
  }

  private static String join(String[] input, String deliminator) {
    if (input.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < input.length - 1; i++) {
      sb.append(input[i]).append(deliminator);
    }
    return sb.toString() + input[input.length - 1];
  }
  
  private String getTmiRequest(String uri) {
    try {
      URL url = new URL(TMI_BASE + uri);
      URLConnection conn = url.openConnection();

      String line;
      String output = "";
      BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), 
          "UTF-8"));

      while ((line = reader.readLine()) != null) {
        // Concatenate read lines
        output = output.concat(line);
      }
      reader.close();

      return output;
    } catch (IOException ex) {
      log.error(String.format("Twitch TMI API raised %s", ex.getMessage()));
      return null;
    }
  }

  @Override
  public List<String> getOnlineMods(String channel) throws IOException {
    String req = getTmiRequest(String.format(CHATTERS, channel.toLowerCase(), clock.getTime()));
    if (req == null) {
      throw new IOException("Could not load chatters list");
    }
    
    ChannelChatters chatters = new Gson().fromJson(req, ChannelChatters.class);
    return chatters.getChatters().getModerators();
  }

  @Override
  public boolean isModerator(String username, String channel) throws IOException {
    List<String> mods = getOnlineMods(channel);
    return mods.contains(username.toLowerCase());
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
}