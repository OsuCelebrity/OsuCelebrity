package me.reddev.osucelebrity.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import me.reddev.osucelebrity.QueueUser;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.twitch.Twitch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tillerino.osuApiModel.OsuApiUser;


public class SpectatorTest {
  @Mock
  private Twitch twitch;
  @Mock
  private Osu osu;
  @Mock
  private CoreSettings settings;

  @Test
  public void testName() throws Exception {
    Spectator spectator = new Spectator(twitch, mock(Clock.class), osu, settings);
    
    assertFalse(spectator.advance());
    
    OsuApiUser apiUser1 = new OsuApiUser();
    apiUser1.setUserId(1);
    assertTrue(spectator.enqueue(new QueueUser(apiUser1, null)));
    // already in queue
    assertFalse(spectator.enqueue(new QueueUser(apiUser1, null)));
    
    spectator.loop();
    
    verify(osu).startSpectate(apiUser1);
    // currently being spectated
    assertFalse(spectator.enqueue(new QueueUser(apiUser1, null)));
  }
  
  @Test
  public void testTiming() throws Exception {
    long[] time = { 0 };
    Clock clock = mock(Clock.class);
    when(clock.getTime()).thenAnswer(x -> time[0]);
    
    when(settings.getDefaultSpecDuration()).thenReturn(30000l);
    
    Spectator spectator = new Spectator(twitch, clock, osu, settings);
    
    spectator.loop();
    
    spectator.enqueue(getUser(1));
    time[0] = 1;
    spectator.enqueue(getUser(2));
    
    spectator.loop();
    
    assertEquals(getUser(1), spectator.currentlySpectating);
    assertEquals(null, spectator.spectatingNext);
    assertEquals(spectator.spectatingUntil, 30001);

    time[0] = 20002;
    spectator.loop();
    
    assertEquals(getUser(1), spectator.currentlySpectating);
    assertEquals(getUser(2), spectator.spectatingNext);
    
    time[0] = 30002;
    spectator.loop();
    
    assertEquals(getUser(2), spectator.currentlySpectating);
    assertEquals(null, spectator.spectatingNext);
}
  
  QueueUser getUser(int id) {
    OsuApiUser apiUser1 = new OsuApiUser();
    apiUser1.setUserId(id);
    return new QueueUser(apiUser1, null);
  }

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }
}
