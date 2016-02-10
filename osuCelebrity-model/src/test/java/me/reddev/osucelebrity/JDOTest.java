package me.reddev.osucelebrity;

import me.reddev.osucelebrity.twitch.TwitchUser;
import me.reddev.osucelebrity.twitchapi.TwitchApiUser;
import org.junit.Test;

public class JDOTest extends AbstractJDOTest {
  @Test
  public void testSetup() throws Exception {
    TwitchUser twitchUser = new TwitchUser(new TwitchApiUser());
    pm.makePersistent(twitchUser);
  }
}
