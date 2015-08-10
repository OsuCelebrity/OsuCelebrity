package me.reddev.osucelebrity;

import me.reddev.osucelebrity.osu.OsuApplication;
import me.reddev.osucelebrity.twitch.TwitchManager;

public class OsuCelebrity {
  private OsuApplication osu;
  private TwitchManager twitchManager;

  public void run() {
    Settings settings = new Settings();

    twitchManager = new TwitchManager(settings, settings);
    twitchManager.start();

    osu = new OsuApplication(settings, settings, settings, twitchManager);
    osu.start();
  }

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    OsuCelebrity mainBot = new OsuCelebrity();
    mainBot.run();
  }
}
