package me.reddev.osucelebrity.twitch;

import org.tillerino.osuApiModel.GameModes;
import static org.junit.Assert.*;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.jdo.PersistenceManager;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.EnqueueResult;
import me.reddev.osucelebrity.core.MockClock;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.VoteType;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuUser;
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
  Osu osu;
  @Mock
  PircBotX bot;
  @Mock
  Configuration<PircBotX> configuration;
  @Mock
  ListenerManager<PircBotX> listenerManager;
  @Mock
  TwitchApi twitchApi;

  TwitchIrcBot ircBot;

  @Before
  public void initMocks() throws Exception {
    when(bot.getConfiguration()).thenReturn(configuration);
    when(configuration.getListenerManager()).thenReturn(listenerManager);
    when(user.getNick()).thenReturn("twitchIrcUser");
    when(user.send()).thenReturn(outputUser);
    when(channel.send()).thenReturn(outputChannel);
    when(spectator.getCurrentPlayer(any()))
        .thenReturn(getUser(pmf.getPersistenceManagerProxy(), "testplayer"));

    when(settings.getTwitchIrcCommand()).thenReturn("!");

    ircBot = new TwitchIrcBot(settings, osuApi, twitchApi, osu, pmf, spectator, clock);
  }
  
  QueuedPlayer getUser(PersistenceManager pm, String playerName) throws IOException {
    OsuUser user = osuApi.getUser(playerName, pm, 0);
    return new QueuedPlayer(user, null, clock.getTime());
  }

  @Test
  public void testQueue() throws Exception {
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!spec someone"));

    verify(spectator).performEnqueue(
        any(),
        eq(new QueuedPlayer(osuApi.getUser("someone", pmf.getPersistenceManagerProxy(), 0),
            QueueSource.TWITCH, 0)), eq("twitch:twitchIrcUser"), any(), any());
  }
  
  @Test
  public void testQueueWithComment() throws Exception {
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!spectate Q__Q    : best emoticon"));

    verify(spectator).performEnqueue(
        any(),
        eq(new QueuedPlayer(osuApi.getUser("Q__Q", pmf.getPersistenceManagerProxy(), 0),
            QueueSource.TWITCH, 0)), eq("twitch:twitchIrcUser"), any(), any());
  }

  @Test
  public void testQueueAlias() throws Exception {
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!vote someone"));

    verify(spectator).performEnqueue(
        any(),
        eq(new QueuedPlayer(osuApi.getUser("someone", pmf.getPersistenceManagerProxy(), 0),
            QueueSource.TWITCH, 0)), eq("twitch:twitchIrcUser"), any(), any());
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
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
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
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!forcespec x"));
    
    verify(spectator).promote(any(), 
        eq(osuApi.getUser("x", pmf.getPersistenceManagerProxy(), 0)));
  }
  
  @Test
  public void testNowPlaying() throws Exception {    
    when(osu.getClientStatus()).thenReturn(
        new OsuStatus(OsuStatus.Type.PLAYING, "Hatsune Miku - Senbonzakura (Short Ver.) [Rin]"));
    
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!np"));
    
    verify(outputChannel).message(any());
  }
  
  @Test
  public void testNowPlayingOnlyWatching() throws Exception {    
    when(osu.getClientStatus()).thenReturn(
        new OsuStatus(OsuStatus.Type.WATCHING, "SomePlayer"));
    
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!np"));
    
    verify(outputChannel, never()).message(any());
  }
  
  @Test
  public void testNotNowPlaying() throws Exception {
    when(osu.getClientStatus()).thenReturn(null);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!np"));
    
    verify(outputChannel, never()).message(any());
  }
  
  @Test
  public void testFixClient() throws Exception {
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!fix"));
    
    verify(osu).restartClient();
  }
  
  @Test
  public void testBoost() throws Exception {
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!boost boosttarget"));
    
    verify(spectator).boost(any(), eq(osuApi.getUser("boosttarget", pmf.getPersistenceManager(), 0)));
  }
  
  @Test
  public void testTimeout() throws Exception {
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!timeout 60 timeouttarget"));
    
    OsuUser target = osuApi.getUser("timeouttarget", pmf.getPersistenceManager(), 0);
    
    assertEquals(60 * 60 * 1000, target.getTimeOutUntil());
  }
  
  @Test
  public void testBanMap() throws Exception {
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!banmaps ban this"));
    
    verify(spectator).addBannedMapFilter(any(), eq("ban this"));
  }
  
  @Test
  public void testChangeGameMode() throws Exception {
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!gamemode taiko player"));
    OsuUser player = osuApi.getUser("player", pmf.getPersistenceManager(), 0);
    assertEquals(GameModes.TAIKO, player.getGameMode());
    
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!gamemode ctb player"));
    player = osuApi.getUser("player", pmf.getPersistenceManager(), 0);
    assertEquals(GameModes.CTB, player.getGameMode());
    
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!gamemode mania player"));
    player = osuApi.getUser("player", pmf.getPersistenceManager(), 0);
    assertEquals(GameModes.MANIA, player.getGameMode());
    
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!gamemode osu player"));
    player = osuApi.getUser("player", pmf.getPersistenceManager(), 0);
    assertEquals(GameModes.OSU, player.getGameMode());
  }
  
  @Test
  public void testExtend() throws Exception {
    when(twitchApi.isModerator(user.getNick())).thenReturn(true);
    ircBot.onMessage(new MessageEvent<PircBotX>(bot, channel, user, "!extend someplayer"));
    
    verify(spectator).extendConditional(any(), eq("someplayer"));
  }
}
