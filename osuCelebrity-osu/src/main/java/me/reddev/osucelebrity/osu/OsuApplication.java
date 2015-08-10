package me.reddev.osucelebrity.osu;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;

import org.tillerino.osuApiModel.OsuApiUser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.twitch.Twitch;

@Slf4j
@RequiredArgsConstructor
public class OsuApplication implements Runnable {
  public interface OsuApplicationSettings {
    int getSpectateDuration();

    String getStreamOutputPath();

    Object getOsuPath();
  }

  private OsuIRCBot _bot;
  private final OsuApplicationSettings _settings;
  private final OsuIrcSettings _ircSettings;
  private final OsuApiSettings _apiSettings;
  private final Twitch _twitch;

  @Getter
  private Process osuProcess;

  /**
   * Connects the Application to the servers
   */
  public void start() {
    _bot = new OsuIRCBot(_ircSettings, _apiSettings, _twitch);
    _bot.start();

    Thread applicationThread = new Thread(this);
    applicationThread.setName("OsuApplication");
    applicationThread.start();
  }

  /**
   * Sends a message to Bancho to start spectating a given user
   * 
   * @param osuUser The username of the Osu! player to spectate
   * @throws IOException Unable to run spectate command
   */
  public void spectate(String osuUser) throws IOException {
    Runtime rt = Runtime.getRuntime();
    String command =
        String.format("\"%s\" \"%s\"", _settings.getOsuPath(),
            String.format(Constants.OSU_COMMAND_SPECTATE, osuUser));
    osuProcess = rt.exec(command);
  }

  public void run() {
    while (true) {
      try {
        TwitchRequest requests = _manager.getRequests();
        OsuApiUser nextUser = requests.getRequestedUsers().poll();

        if (nextUser != null) {
          spectate(nextUser.getUserName());
          _manager.notifySpectate(nextUser.getUserName());

          OsuApiUser after = requests.getRequestedUsers().peek();
          if (after != null)
            _bot.notifyNextPlayer(after.getUserName());

          outputInformation(nextUser, after);
        }

        Thread.sleep(_settings.getSpectateDuration());
      } catch (InterruptedException e) {
        log.error("application thread interrupted");
      } catch (IOException e) {
        log.error("unable to spectate user");
      }
    }
  }

  /**
   * Outputs the current information about the stream
   * 
   * @param currentUser User currently being spectated
   * @param nextUser Next user to be spectated
   */
  public void outputInformation(OsuApiUser currentUser, OsuApiUser nextUser) {
    String outputPath = _settings.getStreamOutputPath();
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
      if (nextUser != null)
        outputFile(outputPath + "next.txt", nextUser.getUserName());

      saveImage("https://a.ppy.sh/" + currentUser.getUserId(), outputPath + "user.png");
      saveImage("https://s.ppy.sh/images/flags/" + currentUser.getCountry().toLowerCase() + ".gif",
          outputPath + "flag.gif");
    } catch (IOException ex) {
      log.error("couldn't output information to files");
    }
  }

  private static void saveImage(String imageUrl, String destinationFile) throws IOException {
    URL url = new URL(imageUrl);
    InputStream is = url.openStream();
    OutputStream os = new FileOutputStream(destinationFile);

    byte[] b = new byte[2048];
    int length;

    while ((length = is.read(b)) != -1) {
      os.write(b, 0, length);
    }

    is.close();
    os.close();
  }

  /**
   * Outputs a message into a basic text file
   * 
   * @param path The path of the output file
   * @param message The message
   * @throws IOException Cannot write to file
   */
  private void outputFile(String path, Object message) throws IOException {
    PrintWriter writer = new PrintWriter(new FileWriter(path));
    writer.write(message.toString());
    writer.close();
  }

  private String getWindowTitle() {
    String line = "";
    try {
      Process p =
          Runtime
              .getRuntime()
              .exec(
                  "cmd /c for /f \"tokens=10 delims=,\" %F in ('tasklist /nh /fi \"imagename eq osu!.exe\" /v /fo csv') do @echo %~F");
      BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
      line = bri.readLine();
      bri.close();
      p.waitFor();
    } catch (Exception err) {
      err.printStackTrace();
    }
    return line;
  }
}
