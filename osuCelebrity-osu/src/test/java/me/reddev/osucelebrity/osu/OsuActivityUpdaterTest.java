package me.reddev.osucelebrity.osu;

import static org.mockito.Mockito.only;

import static org.mockito.Mockito.when;
import org.tillerino.osuApiModel.OsuApiScore;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyInt;
import me.reddev.osucelebrity.osuapi.ApiUser;
import org.tillerino.osuApiModel.GameModes;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import com.querydsl.jdo.JDOQuery;
import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.MockClock;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tillerino.osuApiModel.Downloader;

import javax.jdo.PersistenceManager;

public class OsuActivityUpdaterTest extends AbstractJDOTest {
  @Mock
  Downloader downloader;

  OsuActivityUpdater updater;

  @Before
  public void initMocks() throws Exception {
    updater = new OsuActivityUpdater(downloader, pmf, clock);
  }

  @Test
  public void testCreateMissing() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManagerProxy();

    osuApi.getUser("Tillerino", pm, 0);
    ApiUser user = osuApi.getUserData(0, GameModes.OSU, pm, 0);

    updater.createMissingActivity(pm);

    PlayerActivity activity = pm.getExtent(PlayerActivity.class).iterator().next();

    assertEquals(user, activity.getUser());
  }

  @Test
  public void testUpdateActivity() throws Exception {
    OsuUser user = osuApi.getUser("rank100", pm, 0);
    ApiUser data = osuApi.getUserData(user.getUserId(), 0, pm, 0);
    data.setRank(100);

    updater.createMissingActivity(pm);

    when(downloader.getUserRecent(anyInt(), anyInt(), (Class<OsuApiScore>) any())).thenReturn(
        Collections.emptyList());

    updater.updateActivity(pm);
  }
}
