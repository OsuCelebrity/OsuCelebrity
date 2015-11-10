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
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.output.OutputUser;


public class TwitchIrcBotTest extends AbstractJDOTest {
  @Mock
  TwitchIrcSettings settings;
  @Mock
  CommandHandler<TwitchCommand> handler;
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
  
  MockOsuApi api = new MockOsuApi();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    when(bot.getConfiguration()).thenReturn(configuration);
    when(configuration.getListenerManager()).thenReturn(listenerManager);
    when(user.getNick()).thenReturn("osuIrcUser");
    
    when(settings.getTwitchIrcCommand()).thenReturn("!");
    
    when(user.getNick()).thenReturn("ircUser");
  }

  @Test
  public void testQueue() throws Exception {
    TwitchIrcBot ircBot = new TwitchIrcBot(settings, api, null, pmf);
    ircBot.dispatcher.addHandler(handler);

    MessageEvent<PircBotX> event =
        new MessageEvent<PircBotX>(bot, channel, user, "!queue someone");
    ircBot.onMessage(event);

    verify(handler).handle(
        eq(new QueueUserTwitchCommandImpl(null, "ircUser", api.getUser("someone", 0,
            pmf.getPersistenceManagerProxy(), 0), null)));
  }
}
