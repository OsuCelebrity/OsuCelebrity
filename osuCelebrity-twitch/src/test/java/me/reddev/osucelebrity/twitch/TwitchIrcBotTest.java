package me.reddev.osucelebrity.twitch;

import static org.mockito.Mockito.*;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.CommandHandler;
import me.reddev.osucelebrity.osuapi.MockOsuApi;
import me.reddev.osucelebrity.twitch.commands.QueueUserTwitchCommandImpl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;


public class TwitchIrcBotTest extends AbstractJDOTest {
  @Mock
  TwitchIrcSettings settings;
  @Mock
  CommandHandler<TwitchCommand> handler;
  @Mock
  User user = mock(User.class);
  
  MockOsuApi api = new MockOsuApi();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(settings.getTwitchIrcChannel()).thenReturn("channel");
    when(settings.getTwitchIrcUsername()).thenReturn("name");
    when(settings.getTwitchIrcHost()).thenReturn("host");
    when(settings.getTwitchIrcPort()).thenReturn(1);
    when(settings.getTwitchIrcCommand()).thenReturn("!");
    
    when(user.getNick()).thenReturn("ircUser");
  }

  @Test
  public void testQueue() throws Exception {
    TwitchIrcBot bot = new TwitchIrcBot(settings, api, null, pmf);
    bot.dispatcher.addHandler(handler);

    MessageEvent<PircBotX> event =
        new MessageEvent<PircBotX>(bot.bot, mock(Channel.class), user, "!queue someone");
    bot.onMessage(event);

    verify(handler).handle(
        eq(new QueueUserTwitchCommandImpl(null, "ircUser", api.getUser("someone", 0,
            pmf.getPersistenceManagerProxy(), 0), null)));
  }
}
