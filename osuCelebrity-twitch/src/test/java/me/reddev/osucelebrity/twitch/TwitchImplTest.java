package me.reddev.osucelebrity.twitch;

import org.junit.Before;

import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.SkipReason;
import static org.junit.Assert.*;
import org.junit.Test;
import me.reddev.osucelebrity.core.Trust;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osu.Osu;
import org.mockito.Mock;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;

public class TwitchImplTest extends AbstractJDOTest {
  Twitch twitch;

  @Mock
  private TwitchIrcSettings ircSettings;

  @Mock
  private TwitchApiSettings apiSettings;

  private TwitchIrcBot bot;

  @Mock
  private TwitchApi api;

  @Mock
  private Osu osu;

  @Mock
  private Spectator spectator;

  @Mock
  private TwitchWhisperBot whisperBot;

  @Mock
  private Trust trust;

  @Before
  public void setup() {
    bot =
        new TwitchIrcBot(ircSettings, osuApi, api, osu, pmf, spectator, clock, whisperBot, trust,
            null) {
          @Override
          public void sendMessage(String message) {
            System.out.println(message);
          }
        };

    twitch = new TwitchImpl(osuApi, ircSettings, apiSettings, pmf, bot, api);
  }
  
  @Test
  public void testAdvanceAnnouncement() throws Exception {
    OsuUser oldPlayer = osuUser;
    OsuUser newPlayer = osuUser2;

    twitch.announceAdvance(SkipReason.OFFLINE, oldPlayer, newPlayer);
    twitch.announceAdvance(SkipReason.IDLE, oldPlayer, newPlayer);
    twitch.announceAdvance(SkipReason.BANNED_MAP, oldPlayer, newPlayer);

    twitch.announceAdvance(null, null, newPlayer);
    
    twitch.announceAdvance(null, oldPlayer, newPlayer);
  }
}
