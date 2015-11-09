package me.reddev.osucelebrity;

import me.reddev.osucelebrity.core.Core;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.SystemClock;
import me.reddev.osucelebrity.osu.OsuImpl;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitch.TwitchImpl;

import org.tillerino.osuApiModel.Downloader;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public class OsuCelebrity implements Runnable {
  @Override
  public void run() {
    ExecutorService exec = Executors.newCachedThreadPool();
    
    Settings settings = new Settings();
    
    Properties properties = new Properties();
    try (InputStream is = ClassLoader.getSystemResourceAsStream("persistence.properties")) {
      if (is == null) {
        throw new RuntimeException("persistence.properties not found");
      }
      properties.load(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties, "core");
    
    OsuApi osuApi = new OsuApiImpl(new Downloader(settings.getOsuApiKey()));
    
    OsuImpl osu = new OsuImpl(osuApi, settings, settings, pmf);
    TwitchImpl twitch = new TwitchImpl(osuApi, settings, settings, pmf);
    SystemClock clock = new SystemClock();
    Spectator spectator = new Spectator(twitch, clock, osu, settings, pmf);
    Core core = new Core(osu, twitch, settings, spectator, clock);
    
    List<Future<?>> tasks = new ArrayList<>();

    tasks.add(exec.submit(core));
    tasks.add(exec.submit(osu.getBot()));
    tasks.add(exec.submit(twitch.getBot()));
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
