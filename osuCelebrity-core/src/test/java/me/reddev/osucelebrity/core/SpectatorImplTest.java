package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;

import me.reddev.osucelebrity.osu.PlayerActivity;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitch.Twitch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tillerino.osuApiModel.GameModes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
  
  private SpectatorImpl spectator;

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);

    when(settings.getDefaultSpecDuration()).thenReturn(30000l);
    when(settings.getNextPlayerNotifyTime()).thenReturn(10000l);
    when(settings.getVoteWindow()).thenReturn(30000l);
    when(settings.getOfflineTimeout()).thenReturn(5000L);
    when(settings.getIdleTimeout()).thenReturn(15000L);
    when(settings.getAutoSpecTime()).thenReturn(300000L);
    when(settings.getAutoSpecMaxRank()).thenReturn(1000L);
    when(settings.getAutoSpecMaxLastActivity()).thenReturn(300000L);
    when(osu.getClientStatus()).thenReturn(new OsuStatus(Type.PLAYING, ""));
    
    spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf, api);
  }

  QueuedPlayer getUser(PersistenceManager pm, String playerName) throws IOException {
    OsuUser user = api.getUser(playerName, pm, 0);
    return new QueuedPlayer(user, null, clock.getTime());
  }

  @Test
  public void testEnqueue() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    assertFalse(spectator.advance(pm, PlayerQueue.loadQueue(pm)));

    QueuedPlayer user = getUser(pm, "someplayer");
    assertEquals(EnqueueResult.SUCCESS, spectator.enqueue(pm, user));
    // already in queue
    assertEquals(EnqueueResult.FAILURE, spectator.enqueue(pm, user));

    spectator.loop(pm);
    
    QueuedPlayer user2 = getUser(pm, "someplayer2");
    assertEquals(EnqueueResult.SUCCESS, spectator.enqueue(pm, user2));

    spectator.loop(pm);
    
    QueuedPlayer user3 = getUser(pm, "someplayer3");
    assertEquals(EnqueueResult.SUCCESS, spectator.enqueue(pm, user3));
    
    // we don't want this player to receive a queue or next message, since they are spectated instantly
    verify(osu, times(0)).notifyQueued(user.getPlayer());
    verify(osu, times(0)).notifyNext(user.getPlayer());
    verify(osu).notifyStarting(user.getPlayer());
    
    // we don't want the second player to receive a queue message, since they are already next
    verify(osu, times(0)).notifyQueued(user2.getPlayer());
    verify(osu).notifyNext(user2.getPlayer());
    
    verify(osu).notifyQueued(user3.getPlayer());

    verify(osu).startSpectate(user.getPlayer());
    // currently being spectated
    assertEquals(EnqueueResult.FAILURE, spectator.enqueue(pm, user));
  }

  @Test
  public void testTiming() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

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
  public void testPromote() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator
        .enqueue(pm, new QueuedPlayer(api.getUser("someplayer", pm, 0), null, clock.getTime()));

    spectator.loop(pm);

    OsuUser user2 = api.getUser("someplayer2", pm, 0);

    spectator.promote(pm, user2);

    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    assertEquals(user2, queue.currentlySpectating().get().getPlayer());
  }

  @Test
  public void testPromote2() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator
        .enqueue(pm, new QueuedPlayer(api.getUser("someplayer", pm, 0), null, clock.getTime()));

    spectator.loop(pm);
    clock.sleepUntil(1000);

    spectator
        .enqueue(pm, new QueuedPlayer(api.getUser("someplayer3", pm, 0), null, clock.getTime()));

    spectator.loop(pm);
    clock.sleepUntil(2000);
    
    spectator
        .enqueue(pm, new QueuedPlayer(api.getUser("someplayer2", pm, 0), null, clock.getTime()));

    spectator.loop(pm);
    clock.sleepUntil(3000);

    OsuUser user2 = api.getUser("someplayer2", pm, 0);

    spectator.promote(pm, user2);

    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    assertEquals(user2, queue.currentlySpectating().get().getPlayer());
  }

  @Test
  public void testApprovalUnique() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator
        .enqueue(pm, new QueuedPlayer(api.getUser("someplayer", pm, 0), null, clock.getTime()));

    spectator.loop(pm);

    spectator.enqueue(pm,
        new QueuedPlayer(api.getUser("someplayer2", pm, 0), null, clock.getTime()));

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
    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("someplayer", pm, 0);
    spectator.enqueue(pm, new QueuedPlayer(user, null, clock.getTime()));

    spectator.loop(pm);

    assertEquals(Double.NaN, spectator.getApproval(pm, spectator.getCurrentPlayer(pm)), 0d);
  }

  @Test
  public void testNoVotingDuringStreamDelay() throws Exception {
    when(settings.getStreamDelay()).thenReturn(10000L);

    PersistenceManager pm = pmf.getPersistenceManager();
    assertEquals(
        EnqueueResult.SUCCESS,
        spectator.enqueue(pm,
            new QueuedPlayer(api.getUser("someplayer", pm, 0), null, clock.getTime())));

    spectator.loop(pm);

    assertFalse(spectator.vote(pm, "me", VoteType.UP));
    
    clock.sleepUntil(settings.getStreamDelay());

    assertTrue(spectator.vote(pm, "me", VoteType.UP));
  }

  @Test
  public void testRemainingTime() throws Exception {

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
    assertEquals(30000, spectator.getCurrentPlayer(pm).getStoppingAt());
    spectator.vote(pm, "me", VoteType.UP);

    clock.sleepUntil(20000);
    spectator.loop(pm);
    assertEquals(50000, spectator.getCurrentPlayer(pm).getStoppingAt());

    clock.sleepUntil(40000);
    spectator.loop(pm);
    assertEquals(70000, spectator.getCurrentPlayer(pm).getStoppingAt());

    clock.sleepUntil(60000);
    spectator.loop(pm);
    assertEquals(70000, spectator.getCurrentPlayer(pm).getStoppingAt());
  }
  
  @Test
  public void testFastDrain() throws Exception {

    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("someplayer", pm, 0);
    QueuedPlayer queued = new QueuedPlayer(user, null, clock.getTime());
    spectator.enqueue(pm, queued);

    spectator.loop(pm);
    
    assertEquals(30000, queued.getStoppingAt());
    
    spectator.vote(pm, "negativeNancy", VoteType.DOWN);
    
    clock.sleepUntil(10000);
    spectator.loop(pm);
    
    assertEquals(20000, queued.getStoppingAt());
  }

  @Test
  public void testTimeRefill() throws Exception {

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
    spectator.vote(pm, "someotherguy", VoteType.DOWN);

  }

  @Test
  public void testPlayerOffline() throws Exception {
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

  @Test
  public void testAutoQueue() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    for (int i = 1; i <= 5; i++) {
      OsuUser user = api.getUser("rank" + i, pm, 0);
      if (i == 3) {
        user.setAllowsSpectating(false);
      }
      ApiUser apiUser = api.getUserData(user.getUserId(), GameModes.OSU, pm, 0);
      apiUser.setRank(i);
      pm.makePersistent(new PlayerActivity(apiUser, Long.MAX_VALUE, 0));
    }

    for (int i = 0; i < 10; i++) {
      spectator.loop(pm);
      clock.sleepUntil(clock.getTime() + settings.getAutoSpecTime() / 2);
    }

    // rank 1 is actually spectated twice
    verify(osu, times(2)).startSpectate(api.getUser("rank1", pm, 0));
    verify(osu).startSpectate(api.getUser("rank2", pm, 0));
    // rank 3 is skipped
    verify(osu).startSpectate(api.getUser("rank4", pm, 0));
    verify(osu).startSpectate(api.getUser("rank5", pm, 0));
    verify(osu, times(9)).getClientStatus();
  }

  void testAutoQueueDistribution() throws Exception {
    List<ApiUser> recentlyActive = new ArrayList<>();
    Map<Integer, Long> lastQueueTime = new HashMap<>();
    SpectatorImpl spectator = new SpectatorImpl(twitch, clock, osu, settings, pmf, api) {
      @Override
      List<ApiUser> getRecentlyActive(PersistenceManager pm) {
        return recentlyActive;
      }

      @Override
      Map<Integer, Long> getLastPlayTimes(PersistenceManager pm) {
        return lastQueueTime;
      }
    };

    PersistenceManager pm = pmf.getPersistenceManager();
    clock.sleepUntil(System.currentTimeMillis());

    for (int i = 1; i <= 1000; i++) {
      OsuUser user = api.getUser("rank" + i, pm, 0);
      ApiUser apiUser = api.getUserData(user.getUserId(), GameModes.OSU, pm, 0);
      apiUser.setRank(i);
      recentlyActive.add(apiUser);
    }

    Map<Integer, Integer> queueCount = new LinkedHashMap<>();

    for (int i = 0; i < 100000; i++) {
      OsuUser pick = spectator.pickAutoPlayer(pm, null).get().getPlayer();
      lastQueueTime.put(pick.getUserId(), clock.getTime());
      System.out.println(" " + i);
      queueCount.compute(pick.getUserId(), (k, v) -> v != null ? v + 1 : 1);

      clock.sleepUntil(clock.getTime() + settings.getAutoSpecTime());
    }

    queueCount.forEach((k, v) -> System.out.printf("%d %d%n", k, v));
  }

  public static void main(String[] args) throws Exception {
    SpectatorImplTest test = new SpectatorImplTest();
    test.createDatastore();
    test.initMocks();
    test.testAutoQueueDistribution();
  }

  @Test
  public void testGetQueueSize() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    assertEquals(0, spectator.getQueueSize(pm));
    spectator.enqueue(pm, getUser(pm, "player1"));
    assertEquals(1, spectator.getQueueSize(pm));
    spectator.loop(pm);
    assertEquals(0, spectator.getQueueSize(pm));
    spectator.enqueue(pm, getUser(pm, "player2"));
    spectator.enqueue(pm, getUser(pm, "player3"));
    spectator.enqueue(pm, getUser(pm, "player4"));
    spectator.enqueue(pm, getUser(pm, "player5"));
    spectator.loop(pm);
    assertEquals(4, spectator.getQueueSize(pm));
  }
  
  @Test
  public void testMinPlayCount() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();

    OsuUser user = api.getUser("player", pm, 0);
    api.getUserData(user.getUserId(), 0, pm, 0).setPlayCount(50);
    
    assertEquals(EnqueueResult.FAILURE, spectator.enqueue(pm, new QueuedPlayer(user, QueueSource.TWITCH, 0)));
  }
  
  @Test
  public void testSkipTolerance() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator.enqueue(pm, getUser(pm, "thatname"));
    spectator.loop(pm);
    spectator.enqueue(pm, getUser(pm, "someotherguy"));
    assertTrue(spectator.advanceConditional(pm, "thname"));
  }
  
  @Test
  public void testSkipToleranceFail() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator.enqueue(pm, getUser(pm, "thatname"));
    spectator.loop(pm);
    spectator.enqueue(pm, getUser(pm, "someotherguy"));
    assertFalse(spectator.advanceConditional(pm, "thatotherguy"));
  }
  
  @Test
  public void testQueuePosition() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator.enqueue(pm, getUser(pm, "player1"));
    spectator.enqueue(pm, getUser(pm, "player2"));
    spectator.enqueue(pm, getUser(pm, "player3"));
    assertEquals(2, spectator.getQueuePosition(pm, api.getUser("player2", pm, 0)));
  }
  
  @Test
  public void testQueuePositionNotInQueue() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    spectator.enqueue(pm, getUser(pm, "player1"));
    spectator.enqueue(pm, getUser(pm, "player3"));
    assertEquals(-1, spectator.getQueuePosition(pm, api.getUser("player2", pm, 0)));
  }
}
