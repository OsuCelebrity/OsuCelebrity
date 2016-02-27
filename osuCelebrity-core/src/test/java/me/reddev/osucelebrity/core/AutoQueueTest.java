package me.reddev.osucelebrity.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Matchers.anyBoolean;
import org.mockito.stubbing.OngoingStubbing;
import static org.mockito.Mockito.when;
import org.tillerino.osuApiModel.GameModes;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import me.reddev.osucelebrity.osu.PlayerStatus.PlayerStatusType;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osu.PlayerStatus;
import me.reddev.osucelebrity.osu.Osu.PollStatusConsumer;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import org.mockito.Mock;
import me.reddev.osucelebrity.osu.Osu;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import me.reddev.osucelebrity.AbstractJDOTest;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.jdo.PersistenceManager;

import me.reddev.osucelebrity.osuapi.ApiUser;
import static org.junit.Assert.*;
import org.junit.Test;


public class AutoQueueTest extends AbstractJDOTest {
  AutoQueue autoQueue;

  @Mock
  private Spectator spectator;

  @Mock
  private Osu osu;

  @Mock
  private CoreSettings settings;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    when(settings.getAutoQueueMaxSize()).thenReturn(5);

    autoQueue = new AutoQueue(osu, spectator, clock, pmf, settings);
  }
  
  public static void main(String[] args) throws Exception {
    AbstractJDOTest.createDatastore();
    AutoQueueTest test = new AutoQueueTest();
    test.initMocksOnAbstractJDOTest();
    test.init();
    test.measureDistribution();
  }
  
  public void measureDistribution() throws Exception {
    autoQueue = new AutoQueue(osu, spectator, clock, pmf, settings) {
      @Override
      List<ApiUser> getTopPlayers(PersistenceManager pm) {
        return IntStream.range(1, 1001).mapToObj(rank -> {
          ApiUser apiUser = new ApiUser(rank, 0);
          apiUser.setRank(rank);
          return apiUser;
        }).collect(Collectors.toList());
      }
    };
    int[] counts = new int[1001];
    for (int i = 1; i <= 1000000; i++) {
      counts[autoQueue.drawUserId(pm)]++;
    }
    double sum = IntStream.of(counts).sum();
    for (int i = 0, incSum = 0; i < counts.length; i++) {
      System.out.println(String.format("%d %f %f", i, counts[i] / sum, (incSum += counts[i]) / sum));
    }
  }

  void fillUsers(int ranks) throws Exception {
    for (int i = 1; i <= ranks; i++) {
      ApiUser apiUser =
          osuApi.getUserData(osuApi.getUser("rank" + i, pm, 0L).getUserId(), GameModes.OSU, pm, 0L);
      apiUser.setRank(i);
      pm.makePersistent(apiUser);
    }
  }

  @Test
  public void testPlaying() throws Exception {
    doAnswer(x -> {
      PollStatusConsumer consumer = x.getArgumentAt(1, PollStatusConsumer.class);
      OsuUser player = x.getArgumentAt(0, OsuUser.class);
      PlayerStatus status = new PlayerStatus(player, PlayerStatusType.PLAYING, 0L);
      consumer.accept(pm, status);
      return null;
    }).when(osu).pollIngameStatus(any(), any());
    int[] qSize = {0};
    when(spectator.enqueue(any(), any(), anyBoolean(), any(), anyBoolean())).thenAnswer(x -> {
      qSize[0]++;
      return EnqueueResult.SUCCESS;
    });
    when(spectator.getQueueSize(any())).thenAnswer(x -> qSize[0]);
    fillUsers(100);

    for (int i = 0; i < 10; i++) {
      autoQueue.loop(pm);
    }

    verify(spectator, times(5)).enqueue(eq(pm), any(), eq(false), eq(null), eq(true));
  }

  @Test
  public void testOffline() throws Exception {
    doAnswer(x -> {
      PollStatusConsumer consumer = x.getArgumentAt(1, PollStatusConsumer.class);
      OsuUser player = x.getArgumentAt(0, OsuUser.class);
      PlayerStatus status = new PlayerStatus(player, PlayerStatusType.OFFLINE, 0L);
      consumer.accept(pm, status);
      return null;
    }).when(osu).pollIngameStatus(any(), any());
    fillUsers(100);

    autoQueue.loop(pm);

    verify(spectator, never()).enqueue(eq(pm), any(), eq(false), eq(null), eq(true));
  }
}
