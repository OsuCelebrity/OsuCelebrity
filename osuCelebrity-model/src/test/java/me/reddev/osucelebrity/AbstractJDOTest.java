package me.reddev.osucelebrity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
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
    pmf = JDOHelper.getPersistenceManagerFactory(props, "core");
  }
  
  @After
  public void destroyDatastore() throws SQLException {
    Connection conn = (Connection) pmf.getPersistenceManager().getDataStoreConnection().getNativeConnection();
    conn.createStatement().execute("DROP SCHEMA PUBLIC CASCADE");
    pmf.close();
  }
}
