package me.reddev.osucelebrity.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.commands.QueueSelfOsuCommand;
import me.reddev.osucelebrity.twitch.Twitch;
import org.junit.Test;
import org.tillerino.osuApiModel.OsuApiUser;

public class CoreTest {
  @Test(timeout = 1000)
  public void testQueueSimple() throws Exception {
    Osu osu = mock(Osu.class);
    Twitch twitch = mock(Twitch.class);
    CoreSettings settings = mock(CoreSettings.class);
    Core core = new Core(osu, twitch, settings);
    final Thread thread = new Thread(core);
    thread.start();

    OsuApiUser user = mock(OsuApiUser.class);
    QueueSelfOsuCommand command = mock(QueueSelfOsuCommand.class);
    when(command.getUser()).thenReturn(user);
    core.handleOsuCommand(command);
    verify(osu).startSpectate(user);
    thread.interrupt();
    thread.join();
  }
}
