package me.reddev.osucelebrity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import me.reddev.osucelebrity.osu.OsuImpl;
import me.reddev.osucelebrity.twitch.Twitch;
import me.reddev.osucelebrity.core.Core;

public class OsuCelebrity {
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
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    OsuCelebrity mainBot = new OsuCelebrity();
    mainBot.run();
  }
}
