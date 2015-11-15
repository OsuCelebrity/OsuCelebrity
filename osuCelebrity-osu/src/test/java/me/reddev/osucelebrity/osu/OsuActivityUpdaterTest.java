package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.osuapi.ApiUser;

import org.tillerino.osuApiModel.GameModes;
import static org.junit.Assert.assertEquals;

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
  Clock clock = new MockClock();

  @Mock
  Downloader downloader;

  OsuActivityUpdater updater;

  OsuApi api = new MockOsuApi();

  @Before
  public void initMocks() throws Exception {
    MockitoAnnotations.initMocks(this);

    updater = new OsuActivityUpdater(downloader, pmf, clock);
  }

  @Test
  public void testCreateMissing() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManagerProxy();

    api.getUser("Tillerino", pm, 0);
    ApiUser user = api.getUserData(0, GameModes.OSU, pm, 0);

    updater.createMissingActivity(pm);
    
    PlayerActivity activity = pm.getExtent(PlayerActivity.class).iterator().next();
    
    assertEquals(user, activity.getUser());
  }
}
