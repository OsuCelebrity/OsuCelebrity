package me.reddev.osucelebrity.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.jdo.PersistenceManager;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitch.Twitch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tillerino.osuApiModel.OsuApiUser;


public class SpectatorTest extends AbstractJDOTest {
  @Mock
  private Twitch twitch;
  @Mock
  private Osu osu;
  @Mock
  private CoreSettings settings;
  @Mock
  Clock clock = mock(Clock.class);
  OsuApi api = new MockOsuApi();

  @Test
  public void testName() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    
    Spectator spectator = new Spectator(twitch, clock, osu, settings, pmf);

    assertFalse(spectator.advance(pm));

    QueuedPlayer user = getUser(1);
    assertTrue(spectator.enqueue(pm, user));
    // already in queue
    assertFalse(spectator.enqueue(pm, user));

    spectator.loop(pm);

    verify(osu).startSpectate(user.getPlayer());
    // currently being spectated
    assertFalse(spectator.enqueue(pm, user));
  }

  @Test
  public void testTiming() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    long[] time = {0};
    when(clock.getTime()).thenAnswer(x -> time[0]);

    when(settings.getDefaultSpecDuration()).thenReturn(30000l);

    Spectator spectator = new Spectator(twitch, clock, osu, settings, pmf);

    spectator.loop(pm);

    QueuedPlayer user1 = getUser(1);
    spectator.enqueue(pm, user1);
    time[0] = 1;
    QueuedPlayer user2 = getUser(2);
    spectator.enqueue(pm, user2);

    PlayerQueue queue = PlayerQueue.loadQueue(pm);

    spectator.loop(pm);

    queue = PlayerQueue.loadQueue(pm);
    assertEquals(user1, queue.currentlySpectating().get());
    assertFalse(queue.spectatingNext().isPresent());
    assertEquals(queue.spectatingUntil(), 30001);

    time[0] = 20002;
    spectator.loop(pm);

    assertEquals(user1, queue.currentlySpectating().get());
    assertEquals(user2, queue.spectatingNext().get());

    time[0] = 30002;
    spectator.loop(pm);

    assertEquals(user2, queue.currentlySpectating().get());
    assertFalse(queue.spectatingNext().isPresent());
  }

  QueuedPlayer getUser(int id) {
    OsuApiUser apiUser1 = new OsuApiUser();
    apiUser1.setUserId(id);
    return new QueuedPlayer(new OsuUser(apiUser1, clock.getTime()), null, clock.getTime());
  }

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }
}
