package me.reddev.osucelebrity.twitch;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.output.OutputChannel;
import org.pircbotx.output.OutputUser;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.core.MockClock;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osuapi.MockOsuApi;

public class TwitchWhisperBotTest extends AbstractJDOTest {
  @Mock
  TwitchIrcSettings settings;
  @Mock
  Spectator spectator;

  @Mock
  User user;
  @Mock
  OutputUser outputUser;
  @Mock
  Channel channel;
  @Mock
  OutputChannel outputChannel;
  @Mock
  PircBotX bot;
  @Mock
  Configuration<PircBotX> configuration;
  @Mock
  ListenerManager<PircBotX> listenerManager;

  MockOsuApi api = new MockOsuApi();

  TwitchWhisperBot ircBot;
  
  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(bot.getConfiguration()).thenReturn(configuration);
    when(configuration.getListenerManager()).thenReturn(listenerManager);
    when(user.getNick()).thenReturn("twitchIrcUser");
    when(user.send()).thenReturn(outputUser);
    when(channel.send()).thenReturn(outputChannel);

    when(settings.getTwitchIrcCommand()).thenReturn("!");

    ircBot = new TwitchWhisperBot(settings, api, null, pmf, spectator, new MockClock());
  }
  
  @Test
  public void testGroupServer() throws IOException {
    /*when(ircBot.downloadDirect(any())).thenReturn("{cluster: \"group\","+
        "servers: [\"199.9.253.58:443\"],websockets_servers: [\"192.16.64.212:80\"]}");*/
    
    assertTrue(Pattern.matches("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$", 
        ircBot.getGroupServerHost()));
  }
}
