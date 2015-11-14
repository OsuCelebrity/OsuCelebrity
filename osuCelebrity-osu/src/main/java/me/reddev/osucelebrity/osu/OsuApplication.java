package me.reddev.osucelebrity.osu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.OsuStatus.Type;

import org.tillerino.osuApiModel.OsuApiUser;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
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

    Object getOsuPath();
  }

  private final OsuApplicationSettings settings;
  /**
   * this encoding should be fine and always available.
   */
  public static final Charset CONSOLE_CHARSET = Charset.forName("ISO-8859-1");

  public static final String OSU_COMMAND_SPECTATE = "osu://spectate/%d";

  @Getter
  private Process osuProcess;

  /**
   * Sends a message to Bancho to start spectating a given user.
   * 
   * @param osuUser The username of the Osu! player to spectate
   * @throws IOException Unable to run spectate command
   */
  public void spectate(OsuUser osuUser) throws IOException {
    Runtime rt = Runtime.getRuntime();
    String command =
        String.format("\"%s\" \"%s\"", settings.getOsuPath(),
            String.format(OSU_COMMAND_SPECTATE, osuUser.getUserId()));
    log.debug("issued command " + command);
    osuProcess = rt.exec(command);
  }

  /**
   * Outputs the current information about the stream.
   * 
   * @param currentUser User currently being spectated
   * @param nextUser Next user to be spectated
   */
  public void outputInformation(OsuApiUser currentUser, OsuApiUser nextUser) {
    String outputPath = settings.getStreamOutputPath();
    DecimalFormat thousandsFormatter = new DecimalFormat("###,###");
    DecimalFormat accurateFormatter = new DecimalFormat("###.##");

    try {
      outputFile(outputPath + "player.txt", currentUser.getUserName());
      outputFile(outputPath + "country.txt", currentUser.getCountry());
      outputFile(outputPath + "level.txt", thousandsFormatter.format(currentUser.getLevel()));
      outputFile(outputPath + "pp.txt", accurateFormatter.format(currentUser.getPp()));
      outputFile(outputPath + "accuracy.txt", accurateFormatter.format(currentUser.getAccuracy())
          + "%");
      outputFile(outputPath + "plays.txt", thousandsFormatter.format(currentUser.getPlayCount()));
      outputFile(outputPath + "rank.txt", thousandsFormatter.format(currentUser.getRank()));
      if (nextUser != null) {
        outputFile(outputPath + "next.txt", nextUser.getUserName());
      }

      saveImage("https://a.ppy.sh/" + currentUser.getUserId(), outputPath + "user.png");
      saveImage("https://s.ppy.sh/images/flags/" + currentUser.getCountry().toLowerCase() + ".gif",
          outputPath + "flag.gif");
    } catch (IOException ex) {
      log.error("couldn't output information to files");
    }
  }

  private static void saveImage(String imageUrl, String destinationFile) throws IOException {
    URL url = new URL(imageUrl);
    try (InputStream is = url.openStream(); OutputStream os
      = new FileOutputStream(destinationFile)) {
      byte[] buf = new byte[2048];
      int length;

      while ((length = is.read(buf)) != -1) {
        os.write(buf, 0, length);
      }
    }
  }

  /**
   * Outputs a message into a basic text file.
   * 
   * @param path The path of the output file
   * @param message The message
   * @throws IOException Cannot write to file
   */
  private void outputFile(String path, Object message) throws IOException {
    Writer writer = new OutputStreamWriter(new FileOutputStream(path), CONSOLE_CHARSET);
    writer.write(message.toString());
    writer.close();
  }

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
}
