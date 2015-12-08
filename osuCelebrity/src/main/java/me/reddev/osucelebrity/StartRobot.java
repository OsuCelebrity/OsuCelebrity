package me.reddev.osucelebrity;

import com.google.inject.Guice;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.osu.OsuRobot;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class StartRobot {
  final OsuRobot osuRobot;
  final ScheduledExecutorService exec;

  void main() {
    exec.scheduleWithFixedDelay(osuRobot::findImages, 0, 1, TimeUnit.SECONDS);
  }

  /**
   * runs the download robot.
   */
  public static void main(String[] args) {
    StartRobot robot = Guice.createInjector(new OsuCelebrityModule()).getInstance(StartRobot.class);
    robot.main();
  }
}
