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

  @Before
  public void initMocks() throws IOException {
    OsuApiUser tillerino = new OsuApiUser();
    tillerino.setUserName("Tillerino");
    tillerino.setUserId(2070907);
    OsuApiUser tillerino2 = new OsuApiUser();
    tillerino2.setUserName("Tillerino2");
    tillerino2.setUserId(2070907);
    when(downloader.getUser("Tillerino", 0, OsuApiUser.class)).thenReturn(tillerino, null);
    when(downloader.getUser(2070907, 0, OsuApiUser.class)).thenReturn(tillerino2);

    osuApi = new OsuApiImpl(downloader, clock);
  }

  @Test
  public void testCaching() throws Exception {

    assertNotNull(osuApi.getUser("Tillerino", pm, 0));
    assertNotNull(osuApi.getUser(2070907, pm, 1));
    assertNotNull(osuApi.getUserData(2070907, 0, pm, 1));

    verify(downloader, only()).getUser("Tillerino", 0, OsuApiUser.class);
  }

  @Test
  public void testUpdate() throws Exception {
    assertEquals("Tillerino", osuApi.getUser("Tillerino", pm, 0).getUserName());
    assertEquals("Tillerino", osuApi.getUser(2070907, pm, 0).getUserName());

    clock.sleepUntil(2);

    assertNotNull(osuApi.getUser("Tillerino", pm, 0));
    OsuUser fresh = osuApi.getUser(2070907, pm, 1);
    assertEquals(2, fresh.getDownloaded());
    assertEquals("Tillerino2", fresh.getUserName());
    assertNull(osuApi.getUser("Tillerino", pm, 0));
  }
  
  @Test
  public void testGetIrcUser() throws Exception {
    assertNotNull(osuApi.getIrcUser("Tillerino", pm, 0L).getUser());
  
    when(downloader.getUser(anyString(), anyInt(), any())).thenReturn(null);
    
    clock.sleepUntil(1000);

    assertNotNull(osuApi.getIrcUser("Tillerino", pm, 0L).getUser());
    
    // now update
    assertNull(osuApi.getIrcUser("Tillerino", pm, 1L).getUser());
  }
  
  @Test
  public void testUserData() throws Exception {
    osuApi.getUserData(2070907, 0, pm, 0L);
    
    clock.sleepUntil(1000L);
    
    osuApi.getUserData(2070907, 0, pm, 1L);
  }
}
