package me.reddev.osucelebrity.osu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RequiredArgsConstructor
public class OsuApplication {
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
    try (InputStream is = url.openStream();
        OutputStream os = new FileOutputStream(destinationFile)) {
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

  private String getWindowTitle() {
    String line = "";
    try {
      Process proc =
          Runtime.getRuntime().exec(
              "cmd /c for /f \"tokens=10 delims=,\" %F in"
                  + " ('tasklist /nh /fi \"imagename eq osu!.exe\" /v /fo csv') do @echo %~F");
      BufferedReader bri =
          new BufferedReader(new InputStreamReader(proc.getInputStream(), CONSOLE_CHARSET));
      line = bri.readLine();
      bri.close();
      proc.waitFor();
    } catch (Exception err) {
      err.printStackTrace();
    }
    return line;
  }
}
