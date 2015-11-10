package me.reddev.osucelebrity;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.inject.Guice;


public class OsuCelebrityModuleTest {
  @Test
  public void testInjectionSetup() throws Exception {
    OsuCelebrity osuCeleb = Guice.createInjector(new OsuCelebrityModule()).getInstance(OsuCelebrity.class);
  }
}
