package me.reddev.osucelebrity.core.api;

import me.reddev.osucelebrity.twitch.SceneSwitcher;

import me.reddev.osucelebrity.core.StatusWindow;
import me.reddev.osucelebrity.core.VoteType;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osu.OsuStatus;
import org.mockito.internal.matchers.Contains;
import org.tillerino.osuApiModel.GameModes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.jetty.server.ServerConnector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executors;

import javax.jdo.PersistenceManager;
import javax.ws.rs.core.UriBuilder;

import me.reddev.osucelebrity.AbstractJDOTest;
import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.SpectatorImpl;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.osu.Osu;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.mockito.Mock;


public class CoreApiApplicationTest extends AbstractJDOTest {
  @Mock
  SpectatorImpl spectator;
  @Mock
  CoreSettings settings;
  @Mock
  Osu osu;
  @Mock
  private SceneSwitcher sceneSwitcher;

  @Before
  public void initMocks() throws IOException {
    when(settings.getStreamDelay()).thenReturn(0L);
  }
  
  QueuedPlayer getUser(PersistenceManager pm, String playerName) throws IOException {
    OsuUser user = osuApi.getUser(playerName, pm, 0);
    return new QueuedPlayer(user, null, clock.getTime());
  }
  
  @Test
  public void testCurrentPlayerService() throws Exception {
    URI baseUri = UriBuilder.fromUri("http://localhost/").port(0).build();

    OsuUser user = osuApi.getUser("that player", pmf.getPersistenceManager(), 0);
    user.setGameMode(GameModes.TAIKO);
    
    when(spectator.getCurrentPlayer(any())).thenReturn(
        new QueuedPlayer(user,
            QueueSource.OSU, 0));

    when(spectator.getNextPlayer(any())).thenReturn(
        new QueuedPlayer(osuApi.getUser("next player", pmf.getPersistenceManager(), 0),
            QueueSource.OSU, 0));

    when(osu.getClientStatus()).thenReturn(new OsuStatus(Type.PLAYING, "that beatmap"));

    CurrentPlayerService cps =
        new CurrentPlayerService(pmf, spectator, settings, osu, osuApi,
            Executors.newCachedThreadPool());
    QueueService queueService = new QueueService(pmf, spectator);
    VoteService voteService = new VoteService(pmf, spectator);
    CoreApiApplication apiServerApp = new CoreApiApplication(cps, queueService, voteService);

    Server apiServer =
        JettyHttpContainerFactory
            .createServer(baseUri, ResourceConfig.forApplication(apiServerApp));

    apiServer.start();
    int port = ((ServerConnector) apiServer.getConnectors()[0]).getLocalPort();

    when(spectator.getCurrentQueue(any())).thenReturn(
        Arrays.asList(new DisplayQueuePlayer(1234, "thisguy", "1:45", "12")));

    String result = readUrl(new URL("http://localhost:" + port + "/current"));

    assertThat(result, new Contains("\"name\":\"that player\""));
    assertThat(result, new Contains("\"nextPlayer\":\"next player\""));
    assertThat(result, new Contains("\"beatmap\":\"that beatmap\""));
    assertThat(result, new Contains("\"source\":\"queue\""));
    assertThat(result, new Contains("\"gameMode\":1"));

    System.out.println(queueService.queue());
    result = readUrl(new URL("http://localhost:" + port + "/queue"));

    assertThat(result, new Contains("\"userId\":1234"));
    assertThat(result, new Contains("\"name\":\"thisguy\""));
    assertThat(result, new Contains("\"timeInQueue\":\"1:45\""));
    assertThat(result, new Contains("\"votes\":\"12\""));

    apiServer.stop();
  }
  
  @Test
  public void testVoteService() throws Exception {
    URI baseUri = UriBuilder.fromUri("http://localhost/").port(0).build();
    SpectatorImpl spectatorImpl =
        new SpectatorImpl(null, clock, osu, settings, pmf, osuApi, exec,
            new StatusWindow.DummyStatusWindow(), sceneSwitcher);
    
    QueuedPlayer player = getUser(pm, "some player");
    
    spectatorImpl.enqueue(pm, player, false, "redback", true);
    spectatorImpl.promote(pm, player.getPlayer());
    spectatorImpl.loop();
    
    when(settings.getVoteWindow()).thenReturn(10L);
    
    spectatorImpl.vote(pm, "redback", VoteType.DOWN);
    clock.sleepUntil(1);
    spectatorImpl.vote(pm, "tillerino", VoteType.UP);
    clock.sleepUntil(2);
    spectatorImpl.vote(pm, "redback", VoteType.UP);
    
    spectatorImpl.loop();

    CurrentPlayerService cps =
        new CurrentPlayerService(pmf, spectatorImpl, settings, osu, osuApi,
            Executors.newCachedThreadPool());
    QueueService queueService = new QueueService(pmf, spectatorImpl);
    VoteService voteService = new VoteService(pmf, spectatorImpl);
    
    CoreApiApplication apiServerApp = new CoreApiApplication(cps, queueService, voteService);

    Server apiServer =
        JettyHttpContainerFactory
            .createServer(baseUri, ResourceConfig.forApplication(apiServerApp));

    apiServer.start();
    int port = ((ServerConnector) apiServer.getConnectors()[0]).getLocalPort();
    
    String result = readUrl(new URL("http://localhost:" + port + "/votes"));
    
    assertFalse(result.contains("\"voteType\":\"DOWN\""));
    assertTrue(result.contains("\"voteType\":\"UP\""));
    assertTrue(result.contains("\"twitchUser\":\"redback\""));
    assertTrue(result.contains("\"twitchUser\":\"tillerino\""));
    
    apiServer.stop();
  }

  private String readUrl(URL url) throws IOException, UnsupportedEncodingException {
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    try {
      assertEquals(200, httpCon.getResponseCode());

      InputStream inputStream = httpCon.getInputStream();
      try {
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int len; (len = inputStream.read(buf)) > 0;) {
          baos.write(buf, 0, len);
        }

        return baos.toString("UTF-8");
      } finally {
        inputStream.close();
      }
    } finally {
      httpCon.disconnect();
    }
  }
}
