package me.reddev.osucelebrity.core;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.commands.QueueSelfOsuCommand;
import me.reddev.osucelebrity.twitch.Twitch;
import org.junit.Test;
import org.tillerino.osuApiModel.OsuApiUser;

import java.util.concurrent.CountDownLatch;

public class CoreTest {
  @Test(timeout = 1000)
  public void testQueueSimple() throws Exception {
    Osu osu = mock(Osu.class);
    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(x -> { latch.countDown(); return null; }).when(osu).startSpectate(any());

    Twitch twitch = mock(Twitch.class);
    CoreSettings settings = mock(CoreSettings.class);
    Core core = new Core(osu, twitch, settings);
    final Thread thread = new Thread(core);
    thread.start();

    OsuApiUser user = mock(OsuApiUser.class);
    QueueSelfOsuCommand command = mock(QueueSelfOsuCommand.class);
    when(command.getUser()).thenReturn(user);
    
    core.handleOsuCommand(command);
    latch.await();
    verify(osu).startSpectate(user);
    thread.interrupt();
    thread.join();
  }
}
