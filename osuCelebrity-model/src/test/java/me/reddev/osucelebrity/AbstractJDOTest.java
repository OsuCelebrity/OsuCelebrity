package me.reddev.osucelebrity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jdo.FetchGroup;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.metadata.MemberMetadata;

import org.junit.After;
import org.junit.Before;


public abstract class AbstractJDOTest {
  protected PersistenceManagerFactory pmf;

  @Before
  public void createDatastore() {
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
  public void destroyDatastore() throws SQLException {
    Connection conn =
        (Connection) pmf.getPersistenceManager().getDataStoreConnection().getNativeConnection();
    conn.createStatement().execute("DROP SCHEMA PUBLIC CASCADE");
    pmf.close();
  }
}
