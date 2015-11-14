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


public class SpectatorImplTest extends AbstractJDOTest {
  @Mock
  private Twitch twitch;
  @Mock
  private Osu osu;
  @Mock
  private CoreSettings settings;
  @Mock
  Clock clock = mock(Clock.class);
  OsuApi api = new MockOsuApi();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(settings.getDefaultSpecDuration()).thenReturn(30000l);
    when(settings.getNextPlayerNotifyTime()).thenReturn(10000l);
    when(settings.getVoteWindow()).thenReturn(30000l);
  }

  QueuedPlayer getUser(int id) {
    OsuApiUser apiUser1 = new OsuApiUser();
    apiUser1.setUserId(id);
    return new QueuedPlayer(new OsuUser(apiUser1, clock.getTime()), null, clock.getTime());
  }

  @Test
  public void testEnqueue() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    assertFalse(spectator.advance(pm));

    QueuedPlayer user = getUser(1);
    assertEquals(EnqueueResult.SUCCESS, spectator.enqueue(pm, user));
    // already in queue
    assertEquals(EnqueueResult.FAILURE, spectator.enqueue(pm, user));

    spectator.loop(pm);

    verify(osu).startSpectate(user.getPlayer());
    // currently being spectated
    assertEquals(EnqueueResult.FAILURE, spectator.enqueue(pm, user));
  }

  @Test
  public void testTiming() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    long[] time = {0};
    when(clock.getTime()).thenAnswer(x -> time[0]);

    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

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

  @Test
  public void testApprovalUnique() throws Exception {
    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("someplayer", pm, 0);
    spectator.enqueue(pm, new QueuedPlayer(user, null, clock.getTime()));

    spectator.loop(pm);

    spectator.vote(pm, "spammer", VoteType.UP);
    spectator.vote(pm, "spammer", VoteType.UP);
    spectator.vote(pm, "spammer", VoteType.UP);
    spectator.vote(pm, "spammer", VoteType.UP);
    spectator.vote(pm, "spammer", VoteType.UP);

    spectator.vote(pm, "bummer", VoteType.DOWN);

    assertEquals(.5, spectator.getApproval(pm, spectator.getCurrentPlayer(pm)), 0d);
  }

  @Test
  public void testApprovalLast() throws Exception {
    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("someplayer", pm, 0);
    spectator.enqueue(pm, new QueuedPlayer(user, null, clock.getTime()));

    spectator.loop(pm);

    spectator.vote(pm, "flipflopper", VoteType.UP);
    spectator.vote(pm, "flipflopper", VoteType.DOWN);

    assertEquals(0, spectator.getApproval(pm, spectator.getCurrentPlayer(pm)), 0d);
  }

  @Test
  public void testApprovalNoVotes() throws Exception {
    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("someplayer", pm, 0);
    spectator.enqueue(pm, new QueuedPlayer(user, null, clock.getTime()));

    spectator.loop(pm);

    assertEquals(Double.NaN, spectator.getApproval(pm, spectator.getCurrentPlayer(pm)), 0d);
  }

  @Test
  public void testRemainingTime() throws Exception {
    long[] time = {0};
    when(clock.getTime()).thenAnswer(x -> time[0]);

    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("someplayer", pm, 0);
    spectator.enqueue(pm, new QueuedPlayer(user, null, clock.getTime()));

    spectator.loop(pm);

    {
      QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
      assertEquals(user, currentPlayer.getPlayer());
      assertEquals(30000, currentPlayer.getStoppingAt());
    }

    time[0] = 10000;
    spectator.loop(pm);
    // remaining time is now 20s
    spectator.vote(pm, "me", VoteType.UP);

    for (long t = 10000; t <= 40000; t += 10000) {
      time[0] = t;
      spectator.loop(pm);
      QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
      assertEquals(user, currentPlayer.getPlayer());
      assertEquals(t + 20000, currentPlayer.getStoppingAt());
      assertEquals(clock.getTime(), currentPlayer.getLastRemainingTimeUpdate());
    }
    
    for (long t = 50000; t <= 10000; t += 10000) {
      time[0] = t;
      spectator.loop(pm);
      QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
      assertEquals(user, currentPlayer.getPlayer());
      assertEquals(60000, currentPlayer.getStoppingAt());
      assertEquals(clock.getTime(), currentPlayer.getLastRemainingTimeUpdate());
    }
  }
}
