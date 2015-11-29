package me.reddev.osucelebrity;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.MockClock;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.core.BannedFilter;
import me.reddev.osucelebrity.core.QueueVote;
import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.core.Vote;
import me.reddev.osucelebrity.osu.PlayerActivity;
import me.reddev.osucelebrity.core.QueuedPlayer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.junit.After;


public abstract class AbstractJDOTest {
  static protected PersistenceManagerFactory pmf;
  
  protected OsuApi osuApi = new MockOsuApi();

  protected Clock clock = new MockClock();
  
  protected PersistenceManager pm;
  
  @Mock
  protected ExecutorService exec;

  @Before
  public void initMocksOnAbstractJDOTest() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    when(exec.submit(any(Runnable.class))).thenAnswer(x -> {
      x.getArgumentAt(0, Runnable.class).run();
      return null;
    });
    
    pm = pmf.getPersistenceManager();
  }
  @BeforeClass
  public static void createDatastore() {
    Map<String, String> props = new HashMap<>();
    props.put("javax.jdo.option.ConnectionURL", "jdbc:hsqldb:mem:test");
    props.put("javax.jdo.option.ConnectionDriverName", "org.hsqldb.jdbcDriver");
    props.put("javax.jdo.option.ConnectionUserName", "sa");
    props.put("javax.jdo.option.ConnectionPassword", "");
    props.put("datanucleus.schema.autoCreateAll", "true");
    /*
     * The RetainValues option is important if we verify interaction involving objects which outlive
     * their persistence manager. While this should not happen during normal interaction, it does
     * happen when verifying interactions with mockito.
     */
    props.put("javax.jdo.option.RetainValues", "true");
    pmf = JDOHelper.getPersistenceManagerFactory(props, "core");
  }
  
  @After
  public void truncate() {
    pm.close();
    PersistenceManager pm = pmf.getPersistenceManager();
    pm.getExtent(BannedFilter.class).forEach(pm::deletePersistent);
    pm.getExtent(Vote.class).forEach(pm::deletePersistent);
    pm.getExtent(QueueVote.class).forEach(pm::deletePersistent);
    pm.getExtent(QueuedPlayer.class).forEach(pm::deletePersistent);
    pm.getExtent(PlayerActivity.class).forEach(pm::deletePersistent);
    pm.getExtent(OsuIrcUser.class).forEach(pm::deletePersistent);
    pm.getExtent(ApiUser.class).forEach(pm::deletePersistent);
    pm.getExtent(OsuUser.class).forEach(pm::deletePersistent);
    pm.close();
  }

  @AfterClass
  public static void destroyDatastore() throws SQLException {
    Connection conn =
        (Connection) pmf.getPersistenceManager().getDataStoreConnection().getNativeConnection();
    conn.createStatement().execute("DROP SCHEMA PUBLIC CASCADE");
    pmf.close();
  }
}
