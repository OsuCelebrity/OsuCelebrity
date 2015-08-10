package me.reddev.osucelebrity;

import me.reddev.osucelebrity.core.Core;
import me.reddev.osucelebrity.osu.OsuImpl;
import me.reddev.osucelebrity.twitch.Twitch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class OsuCelebrity implements Runnable {
  @Override
  public void run() {
    ExecutorService exec = Executors.newCachedThreadPool();
    
    Settings settings = new Settings();

    OsuImpl osu = new OsuImpl(settings, settings);
    Twitch twitch = null;
    Core core = new Core(osu, twitch, settings);
    
    List<Future<?>> tasks = new ArrayList<>();

    tasks.add(exec.submit(core));
    tasks.add(exec.submit(osu.getBot()));
  }

  /**
   * Starts the osuCelebrity bot.
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    OsuCelebrity mainBot = new OsuCelebrity();
    mainBot.run();
  }
}
