package me.reddev.osucelebrity.osu;

import java.io.IOException;

import me.reddev.osucelebrity.osu.OsuApplication.OsuApplicationSettings;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.tillerino.osuApiModel.OsuApiUser;

import javax.annotation.CheckForNull;

@Slf4j
public class OsuImpl implements Osu {
  final OsuApi osuApi;
  final OsuIrcSettings ircSettings;
  final OsuApplicationSettings settings;
  
  /**
   * constructs a new Osu instance.
   */
  public OsuImpl(OsuApi osuApi, OsuIrcSettings ircSettings, OsuApplicationSettings settings) {
    super();
    this.osuApi = osuApi;
    this.ircSettings = ircSettings;
    this.settings = settings;
  }

  @CheckForNull
  OsuIrcBot bot;
  
  @CheckForNull
  OsuApplication app;
  
  /**
   * retrieves the irc bot instance.
   * @return might not have been started yet.
   */
  public OsuIrcBot getBot() {
    if (bot == null) {
      bot = new OsuIrcBot(ircSettings, osuApi, this);
    }
    return bot;
  }
  
  /**
   * retrieves the app instance.
   * @return app instance
   */
  public OsuApplication getApp() {
    if (app == null) {
      app = new OsuApplication(settings);
    }
    return app;
  }
  
  @Override
  public void startSpectate(OsuApiUser user) {
    try {
      app.spectate(user.getUserName());
    } catch (IOException e) {
      log.error("error spectating", e);
    }
  }

  @Override
  public void registerCommandHandler(OsuCommandHandler handler) {
    getBot().commandHandlers.add(handler);
  }

  @Override
  public void message(OsuApiUser user, String message) {
    // TODO Auto-generated method stub
    
  }

}
