package me.reddev.osucelebrity;

import com.google.inject.Guice;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.SpectatorImpl;
import me.reddev.osucelebrity.osu.OsuIrcBot;
import me.reddev.osucelebrity.twitch.TwitchIrcBot;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuCelebrity {
  final SpectatorImpl spectator;
  final TwitchIrcBot twitchBot;
  final OsuIrcBot osuBot;

  ExecutorService exec = Executors.newCachedThreadPool();

  void start() {
    exec.submit(spectator);
    exec.submit(twitchBot);
    exec.submit(osuBot);
  }

  /**
   * Starts the osuCelebrity bot.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    OsuCelebrity osuCelebrity =
        Guice.createInjector(new OsuCelebrityModule()).getInstance(OsuCelebrity.class);
    osuCelebrity.start();
  }
}
