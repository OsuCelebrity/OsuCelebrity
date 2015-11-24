package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.core.MockClock;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.osu.OsuApiImpl;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.jdo.PersistenceManager;

import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.osu.OsuUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.OsuApiUser;


public class OsuApiImplTest extends AbstractJDOTest {
  @Mock
  Downloader downloader;

  Clock clock = new MockClock();

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);

    OsuApiUser tillerino = new OsuApiUser();
    tillerino.setUserName("Tillerino");
    tillerino.setUserId(2070907);
    OsuApiUser tillerino2 = new OsuApiUser();
    tillerino2.setUserName("Tillerino2");
    tillerino2.setUserId(2070907);
    when(downloader.getUser("Tillerino", 0, OsuApiUser.class)).thenReturn(tillerino, null);
    when(downloader.getUser(2070907, 0, OsuApiUser.class)).thenReturn(tillerino2);
  }

  @Test
  public void testCaching() throws Exception {
    OsuApiImpl api = new OsuApiImpl(downloader, clock);

    PersistenceManager pm = pmf.getPersistenceManager();

    assertNotNull(api.getUser("Tillerino", pm, 0));
    assertNotNull(api.getUser(2070907, pm, 0));
    assertNotNull(api.getUserData(2070907, 0, pm, 0));

    verify(downloader, only()).getUser("Tillerino", 0, OsuApiUser.class);
  }

  @Test
  public void testUpdate() throws Exception {
    OsuApiImpl api = new OsuApiImpl(downloader, clock);

    PersistenceManager pm = pmf.getPersistenceManager();

    assertEquals("Tillerino", api.getUser("Tillerino", pm, 0).getUserName());
    assertEquals("Tillerino", api.getUser(2070907, pm, 0).getUserName());

    clock.sleepUntil(2);

    assertNotNull(api.getUser("Tillerino", pm, 0));
    OsuUser fresh = api.getUser(2070907, pm, 1);
    assertEquals(2, fresh.getDownloaded());
    assertEquals("Tillerino2", fresh.getUserName());
    assertNull(api.getUser("Tillerino", pm, 0));
  }
}
