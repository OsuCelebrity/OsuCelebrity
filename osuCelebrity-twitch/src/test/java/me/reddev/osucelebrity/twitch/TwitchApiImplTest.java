package me.reddev.osucelebrity.twitch;

import me.reddev.osucelebrity.core.MockClock;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.twitch.TwitchApiImpl;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;

import java.io.IOException;

import me.reddev.osucelebrity.core.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class TwitchApiImplTest extends AbstractJDOTest {
  @Mock
  TwitchApiSettings settings;
  
  Clock clock = new MockClock();

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);
  }
}
