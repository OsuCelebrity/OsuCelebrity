package me.reddev.osucelebrity;

import com.google.inject.Guice;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.core.SpectatorImpl;
import me.reddev.osucelebrity.core.api.CoreApiApplication;
import me.reddev.osucelebrity.osu.OsuActivityUpdater;
import me.reddev.osucelebrity.osu.OsuApplication;
import me.reddev.osucelebrity.osu.OsuIrcBot;
import me.reddev.osucelebrity.osu.OsuRobot;
import me.reddev.osucelebrity.twitch.TwitchApiImpl;
import me.reddev.osucelebrity.twitch.TwitchIrcBot;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.management.ObjectName;
import javax.ws.rs.core.UriBuilder;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuCelebrity {
  final SpectatorImpl spectator;
  final TwitchIrcBot twitchBot;
  final OsuIrcBot osuBot;
  final CoreApiApplication apiServerApp;
  final CoreSettings coreSettings;
  final OsuApplication osuApp;
  final ScheduledExecutorService exec;
  final OsuActivityUpdater osuActivityUpdater;
  final TwitchApiImpl twitchApi;
  final Settings settings;
  final OsuRobot osuRobot;
  
  void start() throws Exception {
    exec.scheduleAtFixedRate(spectator::loop, 0, 100, TimeUnit.MILLISECONDS);
    exec.submit(twitchBot);
    exec.submit(osuBot);
    exec.scheduleAtFixedRate(osuApp::updateWindowTitle, 0, 100, TimeUnit.MILLISECONDS);
    exec.scheduleWithFixedDelay(osuActivityUpdater::update, 0, 5, TimeUnit.SECONDS);
    exec.scheduleWithFixedDelay(twitchApi::updateChatters, 0, 5, TimeUnit.SECONDS);
    exec.scheduleWithFixedDelay(osuRobot::findImages, 0, 1, TimeUnit.SECONDS);

    URI baseUri = UriBuilder.fromUri("http://localhost/").port(coreSettings.getApiPort()).build();

    Server apiServer =
        JettyHttpContainerFactory
            .createServer(baseUri, ResourceConfig.forApplication(apiServerApp));

    apiServer.start();
    
    ManagementFactory.getPlatformMBeanServer().registerMBean(settings,
        new ObjectName("osuCeleb:type=Settings"));
  }

  /**
   * Starts the osuCelebrity bot.
   * 
   * @param args Command line arguments 
   */
  public static void main(String[] args) throws Exception {
    OsuCelebrity osuCelebrity =
        Guice.createInjector(new OsuCelebrityModule()).getInstance(OsuCelebrity.class);
    osuCelebrity.start();
  }
}
