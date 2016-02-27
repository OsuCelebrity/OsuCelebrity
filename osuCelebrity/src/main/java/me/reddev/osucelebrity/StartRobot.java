package me.reddev.osucelebrity;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.osu.OsuRobot;

import com.google.inject.Guice;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class StartRobot {
  final OsuRobot osuRobot;

  void main() {
    Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(osuRobot::findImages, 0, 1,
        TimeUnit.SECONDS);
  }

  /**
   * runs the download robot.
   */
  public static void main(String[] args) {
    StartRobot robot = Guice.createInjector(new OsuCelebrityModule()).getInstance(StartRobot.class);
    robot.main();
  }
}
