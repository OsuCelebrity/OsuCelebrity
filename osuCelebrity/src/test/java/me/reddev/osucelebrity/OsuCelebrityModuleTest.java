package me.reddev.osucelebrity;

import me.reddev.osucelebrity.osu.OsuRobot;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class OsuCelebrityModuleTest {
  @Test
  public void testInjectionSetup() throws Exception {
    Injector injector = Guice.createInjector(new OsuCelebrityModule());
    injector.getInstance(OsuCelebrity.class);
    injector.getInstance(OsuRobot.class);
  }
}
