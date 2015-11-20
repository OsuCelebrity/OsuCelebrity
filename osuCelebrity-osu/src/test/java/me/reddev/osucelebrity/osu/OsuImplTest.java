package me.reddev.osucelebrity.osu;

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

  OsuApi api = new MockOsuApi();
  
  OsuImpl osu;

  @Before
  public void initMocks() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    osu = new OsuImpl(bot, app);
  }
  
  @Test
  public void testLastActivity() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    OsuUser user = api.getUser("activeuser", pm, 0);
    
    pm.makePersistent(new PlayerActivity(api.getUserData(user.getUserId(), 0, pm, 0), 12345, 0));
    
    assertEquals(12345, osu.lastActivity(pm, user));
  }
}
