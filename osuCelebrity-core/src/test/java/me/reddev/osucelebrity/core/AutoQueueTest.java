package me.reddev.osucelebrity.core;

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

import java.util.concurrent.Semaphore;

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

  void fillUsers() throws Exception {
    for (int i = 1; i <= 100; i++) {
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
    fillUsers();

    for (int i = 0; i < 10; i++) {
      autoQueue.loop();
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
    fillUsers();

    autoQueue.loop();

    verify(spectator, never()).enqueue(eq(pm), any(), eq(false), eq(null), eq(true));
  }
}
