package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.core.Spectator;

import static org.junit.Assert.assertEquals;

import javax.jdo.PersistenceManager;

import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Before;
import me.reddev.osucelebrity.AbstractJDOTest;
import org.junit.Test;


public class OsuImplTest extends AbstractJDOTest {
  @Mock
  OsuIrcBot bot;
  
  @Mock
  OsuApplication app;
  
  @Mock
  Spectator spectator;

  OsuImpl osu;

  @Before
  public void initMocks() throws Exception {
    osu = new OsuImpl(bot, app, spectator, pmf);
  }
  
  @Test
  public void testLastActivity() throws Exception {
    OsuUser user = osuApi.getUser("activeuser", pm, 0);
    
    pm.makePersistent(new PlayerActivity(osuApi.getUserData(user.getUserId(), 0, pm, 0), 12345, 0));
    
    assertEquals(12345, osu.lastActivity(pm, user));
  }
}
