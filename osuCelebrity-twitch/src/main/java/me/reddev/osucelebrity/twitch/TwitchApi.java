package me.reddev.osucelebrity.twitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
@RequiredArgsConstructor
public class TwitchApi {
  private final TwitchApiSettings settings;

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
}
