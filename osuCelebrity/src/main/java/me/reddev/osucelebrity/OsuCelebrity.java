package me.reddev.osucelebrity;

import com.google.inject.Guice;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.core.SpectatorImpl;
import me.reddev.osucelebrity.core.api.CoreApiApplication;
import me.reddev.osucelebrity.osu.OsuApplication;
import me.reddev.osucelebrity.osu.OsuIrcBot;
import me.reddev.osucelebrity.twitch.TwitchIrcBot;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuCelebrity {
  final SpectatorImpl spectator;
  final TwitchIrcBot twitchBot;
  final OsuIrcBot osuBot;
  final CoreApiApplication apiServerApp;
  final CoreSettings coreSettings;
  final OsuApplication osuApp;
  final ExecutorService exec;

  void start() {
    exec.submit(spectator);
    exec.submit(twitchBot);
    exec.submit(osuBot);
    exec.submit(osuApp);

    URI baseUri = UriBuilder.fromUri("http://localhost/").port(coreSettings.getApiPort()).build();

    Server apiServer =
        JettyHttpContainerFactory
            .createServer(baseUri, ResourceConfig.forApplication(apiServerApp));

    try {
      apiServer.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Starts the osuCelebrity bot.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    OsuCelebrity osuCelebrity =
        Guice.createInjector(new OsuCelebrityModule()).getInstance(OsuCelebrity.class);
    osuCelebrity.start();
  }
}
