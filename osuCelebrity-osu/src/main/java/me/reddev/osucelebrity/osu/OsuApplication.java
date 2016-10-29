package me.reddev.osucelebrity.osu;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import com.github.omkelderman.osudbparser.OsuBeatmapInfo;
import com.github.omkelderman.osudbparser.OsuDbFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.OsuStatus.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuApplication {
  private static final String OSU = "osu!(?:cuttingedge \\S+)?\\s*";
  static Pattern nothingPattern = Pattern.compile(OSU);
  static Pattern playingPattern = Pattern.compile(OSU + "- (.*)");
  static Pattern watchingPattern = Pattern.compile(OSU + "-  \\(watching (.*)\\)");

  public interface OsuApplicationSettings {
    int getOsuClientXOffset();
    
    int getOsuClientYOffset();
    
    int getOsuClientWidth();
    
    int getOsuClientHeight();
    
    String getOsuDbPath();
  }

  private final OsuApplicationSettings settings;
  /**
   * this encoding should be fine and always available.
   */
  public static final Charset CONSOLE_CHARSET = Charset.forName("ISO-8859-1");

  public static final String OSU_COMMAND_SPECTATE = "osu://spectate/%d";
  
  String[] getSpecCommand(int userid) {
    String[] cmd = {
        "cmd.exe",
        "/c",
        "start",
        String.format(OSU_COMMAND_SPECTATE, userid)
    };
    return cmd;
  }
  
  /**
   * Sends a message to Bancho to start spectating a given user.
   * 
   * @param osuUser The username of the Osu! player to spectate
   * @throws IOException Unable to run spectate command
   */
  public void spectate(OsuUser osuUser) throws IOException {
    log.debug("spectating {}", osuUser);
    ProcessBuilder builder = new ProcessBuilder(getSpecCommand(6854947));
    builder.inheritIO();
    builder.start();
    
    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      return;
    }
    
    builder = new ProcessBuilder(getSpecCommand(osuUser.getUserId()));
    builder.inheritIO();
    builder.start();
  }

  @CheckForNull
  String windowTitle = null;
  volatile Multimap<String, OsuBeatmapInfo> beatmaps = Multimaps.forMap(Collections.emptyMap());

  /**
   * updates the osu client window title. logs exceptions.
   */
  public void updateWindowTitle() {
    try {
      windowTitle = readWindowTitle();
    } catch (InterruptedException e) {
      return;
    } catch (Exception e) {
      log.error("exception while updating window title", e);
    }
  }
  
  /**
   * Parses the current osu database.
   */
  public void updateOsuDb() {
    try {
      OsuDbFile db = OsuDbFile.parse(settings.getOsuDbPath());
      Multimap<String, OsuBeatmapInfo> beatmaps =
          Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
      Stream.of(db.getBeatmaps()).forEach(
          map -> beatmaps.put(
              String.format("%s - %s [%s]", map.getArtistName(), map.getSongTitle(),
                  map.getDifficulty()), map));
      this.beatmaps = beatmaps;
    } catch (Exception e) {
      log.error("error while reading osu db", e);
    }
  }

  @CheckForNull
  String readWindowTitle() throws InterruptedException {
    try {
      Process proc =
          Runtime.getRuntime().exec("cmd /c tasklist /nh /fi \"imagename eq osu!.exe\" /v /fo csv");
      try (BufferedReader bri =
          new BufferedReader(new InputStreamReader(proc.getInputStream(), CONSOLE_CHARSET))) {
        String line = bri.readLine();
        if (line == null) {
          return null;
        }
        String[] split = line.split("\",\"");
        if (split.length <= 1) {
          return null;
        }
        String name = split[split.length - 1];
        return name.substring(0, name.length() - 1);
      } finally {
        proc.waitFor();
      }
    } catch (IOException err) {
      log.error("error while getting osu client window title", err);
      return null;
    }
  }

  OsuStatus getStatus() {
    String title = windowTitle;
    if (title == null) {
      return new OsuStatus(Type.CLOSED, null);
    }

    {
      Matcher matcher = watchingPattern.matcher(title);
      if (matcher.matches()) {
        return new OsuStatus(Type.WATCHING, matcher.group(1));
      }
    }

    {
      Matcher matcher = playingPattern.matcher(title);
      if (matcher.matches()) {
        return new OsuStatus(Type.PLAYING, matcher.group(1));
      }
    }

    {
      Matcher matcher = nothingPattern.matcher(title);
      if (matcher.matches()) {
        return new OsuStatus(Type.IDLE, null);
      }
    }

    log.warn("Can't parse title \"{}\"", title);
    return new OsuStatus(Type.UNKNOWN, null);
  }
  
  /**
   * kills the osu client via command line.
   */
  public void killOsu() throws IOException {
    Runtime rt = Runtime.getRuntime();
    String command = "taskkill /F /IM osu!.exe";
    log.debug("killing osu: " + command);
    rt.exec(command);
  }
}
