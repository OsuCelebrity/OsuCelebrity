package me.reddev.osucelebrity.osu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.OsuApplication.OsuApplicationSettings;
import me.reddev.osucelebrity.osuapi.OsuApi;

import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor
public class OsuImpl implements Osu {
  final OsuApi osuApi;
  final OsuIrcSettings ircSettings;
  final OsuApplicationSettings settings;
  final PersistenceManagerFactory pmf;

  @CheckForNull
  OsuIrcBot bot;

  @CheckForNull
  OsuApplication app;

  /**
   * retrieves the irc bot instance.
   * 
   * @return might not have been started yet.
   */
  public OsuIrcBot getBot() {
    if (bot == null) {
      bot = new OsuIrcBot(ircSettings, osuApi, this, pmf);
    }
    return bot;
  }

  /**
   * retrieves the app instance.
   * 
   * @return app instance
   */
  public OsuApplication getApp() {
    if (app == null) {
      app = new OsuApplication(settings);
    }
    return app;
  }

  @Override
  public void startSpectate(OsuUser user) {
    try {
      getApp().spectate(user);
    } catch (IOException e) {
      log.error("error spectating", e);
    }
  }

  @Override
  public void registerCommandHandler(OsuCommandHandler handler) {
    getBot().dispatcher.addHandler(handler);
  }

  @Override
  public void message(OsuUser user, String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void notifyStarting(OsuUser user) {
    // spaces are replaced with underscores on IRC
    getBot().notifyStartingPlayer(user.getUserName().replace(" ", "_"));
  }

  @Override
  public void notifySoon(OsuUser player) {
    // implement and make consistent #notifyUpcoming
    throw new UnsupportedOperationException();
  }
}
