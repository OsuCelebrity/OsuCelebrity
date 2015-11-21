package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.snapshot.UserSnapshot;
import org.pircbotx.snapshot.UserChannelDaoSnapshot;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.JoinEvent;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.HashSet;

import org.pircbotx.hooks.events.ServerResponseEvent;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.jdo.PersistenceManager;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.Privilege;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.core.EnqueueResult;
import me.reddev.osucelebrity.core.MockClock;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.output.OutputUser;


public class OsuIrcBotTest extends AbstractJDOTest {
  MockClock clock = new MockClock();
  OsuApi osuApi = new MockOsuApi();

  @Mock
  Spectator spectator;

  @Mock
  User user;
  @Mock
  OutputUser outputUser;
  @Mock
  Channel channel;
  @Mock
  PircBotX bot;
  @Mock
  Configuration<PircBotX> configuration;
  @Mock
  ListenerManager<PircBotX> listenerManager;
  @Mock
  OsuIrcSettings settings;

  OsuIrcBot ircBot;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(bot.getConfiguration()).thenReturn(configuration);
    when(configuration.getListenerManager()).thenReturn(listenerManager);
    when(user.getNick()).thenReturn("osuIrcUser");

    when(settings.getOsuIrcCommand()).thenReturn("!");
    when(user.send()).thenReturn(outputUser);

    ircBot = new OsuIrcBot(null, osuApi, settings, pmf, spectator, clock);
  }

  @Test
  public void testSelfQueue() throws Exception {
    when(spectator.enqueue(any(), any())).thenReturn(EnqueueResult.SUCCESS);

    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!spec"));

    ArgumentCaptor<QueuedPlayer> captor = ArgumentCaptor.forClass(QueuedPlayer.class);

    verify(spectator, only()).enqueue(any(), captor.capture());

    QueuedPlayer request = captor.getValue();
    assertEquals("osuIrcUser", request.getPlayer().getUserName());
    assertEquals(true, request.isNotify());

    verify(outputUser, only()).message(Responses.SELF_QUEUE_SUCCESSFUL);
  }

  @Test
  public void testSkip() throws Exception {
    PersistenceManager pm = pmf.getPersistenceManager();
    osuApi.getUser("osuIrcUser", pm, 0).setPrivilege(Privilege.MOD);

    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!forceskip x"));

    verify(spectator, only()).advanceConditional(any(),
        eq("x"));

    verify(outputUser, only()).message(any());
  }

  @Test
  public void testSkipUnauthorized() throws Exception {
    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!forceskip x"));

    verifyZeroInteractions(spectator);

    verify(outputUser, only()).message(any());
  }

  @Test
  public void testMuting() throws Exception {
    assertTrue(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0).isAllowsNotifications());

    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!mute"));

    assertFalse(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0)
        .isAllowsNotifications());

    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!unmute"));

    assertTrue(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0).isAllowsNotifications());
  }

  @Test
  public void testOpting() throws Exception {
    assertTrue(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0).isAllowsSpectating());

    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!optout"));

    assertFalse(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0).isAllowsSpectating());
    verify(spectator).removeFromQueue(any(),
        eq(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0)));

    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!optin"));

    assertTrue(osuApi.getUser("osuIrcUser", pmf.getPersistenceManager(), 0).isAllowsSpectating());
  }

  @Test
  public void testUserNamesParser() throws Exception {
    ircBot.onServerResponse(new ServerResponseEvent<PircBotX>(bot, 353,
        ":irc.server.net 353 Phyre = #SomeChannel :@me +you them", ImmutableList
            .copyOf(new String[] {"#SomeChannel", "+me @you them"})));

    assertEquals(new HashSet<>(Arrays.asList("me", "you", "them")), ircBot.getOnlineUsers());
  }

  @Test
  public void testJoinQuit() throws Exception {
    ircBot.onJoin(new JoinEvent<PircBotX>(bot, channel, user));

    assertTrue(ircBot.getOnlineUsers().contains("osuIrcUser"));

    ircBot.onQuit(new QuitEvent<PircBotX>(bot, new UserChannelDaoSnapshot(bot, null, null, null,
        null, null, null), new UserSnapshot(user), "no reason"));

    assertFalse(ircBot.getOnlineUsers().contains("osuIrcUser"));
  }

  @Test
  public void testQueue() throws Exception {
    when(spectator.enqueue(any(), any())).thenReturn(EnqueueResult.SUCCESS);
    
    ircBot.onPrivateMessage(new PrivateMessageEvent<PircBotX>(bot, user, "!spec thatguy"));
    
    verify(spectator, only()).enqueue(
        any(),
        eq(new QueuedPlayer(osuApi.getUser("thatguy", pmf.getPersistenceManager(), 0),
            QueueSource.OSU, 0)));
  }
}
