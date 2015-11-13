package me.reddev.osucelebrity.osu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.OsuApplication.OsuApplicationSettings;
import me.reddev.osucelebrity.osuapi.OsuApi;

import java.io.IOException;

import javax.inject.Inject;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuImpl implements Osu {
  final OsuApi osuApi;
  final OsuIrcSettings ircSettings;
  final OsuApplicationSettings settings;
  final PersistenceManagerFactory pmf;
  final OsuIrcBot bot;
  final OsuApplication app;

  @Override
  public void startSpectate(OsuUser user) {
    try {
      app.spectate(user);
    } catch (IOException e) {
      log.error("error spectating", e);
    }
  }

  @Override
  public void message(OsuUser user, String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void notifyStarting(OsuUser user) {
    bot.notifyStartingPlayer(user);
  }

  @Override
  public void notifySoon(OsuUser player) {
    // implement and make consistent #notifyUpcoming
    throw new UnsupportedOperationException();
  }
  
  @Override
  public OsuStatus getClientStatus() {
    return app.getStatus();
  }
}
