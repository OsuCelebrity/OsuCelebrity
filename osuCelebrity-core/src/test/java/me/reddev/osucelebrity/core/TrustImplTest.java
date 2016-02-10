package me.reddev.osucelebrity.core;

import static org.mockito.Mockito.when;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.twitch.TwitchUser;
import me.reddev.osucelebrity.twitchapi.TwitchApiUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class TrustImplTest extends AbstractJDOTest {
  TrustImpl trust;
  
  @Mock
  CoreSettings settings;

  @Before
  public void initMocks() throws Exception {
    trust = new TrustImpl(clock, settings);
    
    when(settings.getTwitchTrustAccountAge()).thenReturn(14L * 24 * 60 * 60 * 1000);
  }
  
  @Test(expected = UserException.class)
  public void testAlreadyUntrusted() throws Exception {
    TwitchUser user = new TwitchUser(null);
    user.setTrusted(false);
    trust.checkTrust(pm, user);
  }
  
  @Test(expected = UserException.class)
  public void testNewAccount() throws Exception {
    TwitchApiUser apiUser = new TwitchApiUser();
    apiUser.setCreatedAt(new Date(0));
    
    clock.sleepUntil(1000);
    
    trust.checkTrust(pm, new TwitchUser(apiUser));
  }
  
  @Test
  public void testOldAccount() throws Exception {
    TwitchApiUser apiUser = new TwitchApiUser();
    apiUser.setCreatedAt(new Date(0));
    
    clock.sleepUntil(settings.getTwitchTrustAccountAge() + 1000);
    
    trust.checkTrust(pm, new TwitchUser(apiUser));
  }
}
