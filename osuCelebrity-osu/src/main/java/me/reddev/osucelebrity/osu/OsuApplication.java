package me.reddev.osucelebrity.osu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.OsuStatus.Type;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuApplication implements Runnable {
  static Pattern playingPattern = Pattern.compile("osu!  - (.*)");
  static Pattern watchingPattern = Pattern.compile("osu!  -  \\(watching (.*)\\)");

  public interface OsuApplicationSettings {
    String getStreamOutputPath();
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
    log.debug("spectating {}" + osuUser);
    ProcessBuilder builder = new ProcessBuilder(getSpecCommand(6854947));
    builder.inheritIO();
    builder.start();
    
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      return;
    }
    
    builder = new ProcessBuilder(getSpecCommand(osuUser.getUserId()));
    builder.inheritIO();
    builder.start();
  }

  @CheckForNull
  String windowTitle = null;

  @Override
  public void run() {
    try {
      for (;;) {
        try {
          windowTitle = readWindowTitle();
          Thread.sleep(100);
        } catch (InterruptedException e) {
          return;
        }
      }
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }

  /**
   * Retrieves the window title.
   * 
   * @return the window title or null if there is no window
   */
  @CheckForNull
  public String getWindowTitle() {
    return windowTitle;
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

  @CheckForNull
  OsuStatus getStatus() {
    String title = getWindowTitle();
    if (title == null) {
      return null;
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

    return null;
  }
  
  /**
   * kills the osu client via command line.
   */
  public void killOsu() throws IOException {
    Runtime rt = Runtime.getRuntime();
    String command = "taskkill /F /FI \"WINDOWTITLE eq osu!*\"";
    log.debug("killing osu: " + command);
    rt.exec(command);
  }
}
