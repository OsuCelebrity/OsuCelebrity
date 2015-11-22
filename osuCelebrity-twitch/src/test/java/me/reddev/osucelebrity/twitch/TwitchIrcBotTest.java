package me.reddev.osucelebrity.twitch;

import static org.mockito.Mockito.*;
import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.core.EnqueueResult;
import me.reddev.osucelebrity.core.MockClock;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.VoteType;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.osuapi.MockOsuApi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.output.OutputChannel;
import org.pircbotx.output.OutputUser;


public class TwitchIrcBotTest extends AbstractJDOTest {
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

  TwitchIrcBot ircBot;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(bot.getConfiguration()).thenReturn(configuration);
    when(configuration.getListenerManager()).thenReturn(listenerManager);
    when(user.getNick()).thenReturn("twitchIrcUser");
    when(user.send()).thenReturn(outputUser);
    when(channel.send()).thenReturn(outputChannel);

    when(settings.getTwitchIrcCommand()).thenReturn("!");

    ircBot = new TwitchIrcBot(settings, api, null, pmf, spectator, new MockClock());
  }

  @Test
  public void testQueue() throws Exception {
    when(spectator.enqueue(any(), any(), eq(false))).thenReturn(EnqueueResult.SUCCESS);

    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!spec someone"));

    verify(spectator).enqueue(
        any(),
        eq(new QueuedPlayer(api.getUser("someone", pmf.getPersistenceManagerProxy(), 0),
            QueueSource.TWITCH, 0)), eq(false));
    verify(outputChannel).message(anyString());
  }

  @Test
  public void testDank() throws Exception {
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!dank"));

    verify(spectator).vote(any(), eq("twitchIrcUser"), eq(VoteType.UP));
  }

  @Test
  public void testSkip() throws Exception {
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!skip"));

    verify(spectator).vote(any(), eq("twitchIrcUser"), eq(VoteType.DOWN));
  }

  @Test
  public void testForceSkip() throws Exception {
    when(channel.isOp(user)).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!forceskip x"));

    verify(spectator).advanceConditional(any(),
        eq("x"));
  }

  @Test
  public void testForceSkipUnauthorized() throws Exception {
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!forceskip x"));

    verifyNoMoreInteractions(spectator);
  }
  
  @Test
  public void testForceSpec() throws Exception {
    when(channel.isOp(user)).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!forcespec x"));
    
    verify(spectator).promote(any(), 
        eq(api.getUser("x", pmf.getPersistenceManagerProxy(), 0)));
  }
}
