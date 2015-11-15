package me.reddev.osucelebrity.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitch.Twitch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import javax.jdo.PersistenceManager;


public class SpectatorImplTest extends AbstractJDOTest {
  @Mock
  private Twitch twitch;
  @Mock
  private Osu osu;
  @Mock
  private CoreSettings settings;
  
  Clock clock = new MockClock();
  OsuApi api = new MockOsuApi();

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);

    when(settings.getDefaultSpecDuration()).thenReturn(30000l);
    when(settings.getNextPlayerNotifyTime()).thenReturn(10000l);
    when(settings.getVoteWindow()).thenReturn(30000l);
    when(settings.getOfflineTimeout()).thenReturn(5000L);
    when(settings.getIdleTimeout()).thenReturn(15000L);
  }

  QueuedPlayer getUser(PersistenceManager pm, String playerName) throws IOException {
    OsuUser user = api.getUser(playerName, pm, 0);
    return new QueuedPlayer(user, null, clock.getTime());
  }

  @Test
  public void testEnqueue() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    assertFalse(spectator.advance(PlayerQueue.loadQueue(pm)));

    QueuedPlayer user = getUser(pm, "someplayer");
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

    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    spectator.loop(pm);

    QueuedPlayer user1 = getUser(pm, "someplayer");
    spectator.enqueue(pm, user1);
    clock.sleepUntil(1);
    QueuedPlayer user2 = getUser(pm, "someplayer2");
    spectator.enqueue(pm, user2);

    PlayerQueue queue = PlayerQueue.loadQueue(pm);

    spectator.loop(pm);

    queue = PlayerQueue.loadQueue(pm);
    assertEquals(user1, queue.currentlySpectating().get());
    assertFalse(queue.spectatingNext().isPresent());
    assertEquals(queue.spectatingUntil(), 30001);

    clock.sleepUntil(20002);
    spectator.loop(pm);

    assertEquals(user1, queue.currentlySpectating().get());
    assertEquals(user2, queue.spectatingNext().get());

    clock.sleepUntil(30002);
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

    clock.sleepUntil(10000);
    spectator.loop(pm);
    // remaining time is now 20s
    spectator.vote(pm, "me", VoteType.UP);

    for (long t = 10000; t <= 40000; t += 10000) {
      clock.sleepUntil(t);
      spectator.loop(pm);
      QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
      assertEquals(user, currentPlayer.getPlayer());
      assertEquals(t + 20000, currentPlayer.getStoppingAt());
      assertEquals(clock.getTime(), currentPlayer.getLastRemainingTimeUpdate());
    }

    for (long t = 50000; t <= 10000; t += 10000) {
      clock.sleepUntil(t);
      spectator.loop(pm);
      QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
      assertEquals(user, currentPlayer.getPlayer());
      assertEquals(60000, currentPlayer.getStoppingAt());
      assertEquals(clock.getTime(), currentPlayer.getLastRemainingTimeUpdate());
    }
  }

  @Test
  public void testPlayerOffline() throws Exception {
    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    PersistenceManager pm = pmf.getPersistenceManager();

    spectator.enqueue(pm, getUser(pm, "user1"));
    QueuedPlayer player2 = getUser(pm, "user2");
    spectator.enqueue(pm, player2);
    spectator.enqueue(pm, getUser(pm, "user3"));

    when(osu.getClientStatus()).thenReturn(null);
    spectator.loop(pm);

    clock.sleepUntil(1000);
    spectator.loop(pm);
    assertEquals("user1", spectator.getCurrentPlayer(pm).getPlayer().getUserName());

    when(osu.getClientStatus()).thenReturn(new OsuStatus(Type.WATCHING, ""));

    for (; clock.getTime() < 10000; clock.sleepUntil(clock.getTime() + 1000)) {
      spectator.loop(pm);
      assertEquals("user1", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    }

    when(osu.getClientStatus()).thenReturn(new OsuStatus(Type.PLAYING, ""));

    for (; clock.getTime() < settings.getDefaultSpecDuration(); clock
        .sleepUntil(clock.getTime() + 1000)) {
      spectator.loop(pm);
      assertEquals("user1", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    }

    spectator.loop(pm);
    assertEquals("user2", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    
    when(osu.getClientStatus()).thenReturn(null);
    
    for (; clock.getTime() < settings.getDefaultSpecDuration() + settings.getOfflineTimeout(); clock
        .sleepUntil(clock.getTime() + 1000)) {
      spectator.loop(pm);
      assertEquals("user2", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    }
    
    spectator.loop(pm);
    
    assertEquals("user3", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    
    verify(twitch).announcePlayerSkipped(SkipReason.OFFLINE, player2.getPlayer());
  }

  @Test
  public void testPlayerIdle() throws Exception {
    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf);

    PersistenceManager pm = pmf.getPersistenceManager();

    QueuedPlayer player1 = getUser(pm, "user1");
    spectator.enqueue(pm, player1);
    spectator.enqueue(pm, getUser(pm, "user2"));
    spectator.loop(pm);
    
    when(osu.getClientStatus()).thenReturn(new OsuStatus(Type.WATCHING, ""));
    for (; clock.getTime() < settings.getIdleTimeout(); clock.sleepUntil(clock.getTime() + 1000)) {
      spectator.loop(pm);
      assertEquals("user1", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    }

    spectator.loop(pm);

    assertEquals("user2", spectator.getCurrentPlayer(pm).getPlayer().getUserName());
    
    verify(twitch).announcePlayerSkipped(SkipReason.IDLE, player1.getPlayer());
  }
}
