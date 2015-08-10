package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.osuapi.OsuApi;

import me.reddev.osucelebrity.osuapi.OsuApiSettings;
import org.tillerino.osuApiModel.OsuApiUser;

import javax.annotation.CheckForNull;

public class OsuImpl implements Osu {
  final OsuApi osuApi;
  final OsuIrcSettings ircSettings;
  
  /**
   * constructs a new Osu instance.
   */
  public OsuImpl(OsuApi osuApi, OsuIrcSettings ircSettings) {
    super();
    this.osuApi = osuApi;
    this.ircSettings = ircSettings;
  }

  @CheckForNull
  OsuIrcBot bot;
  
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
  
  @Override
  public void startSpectate(OsuApiUser user) {
    // TODO Auto-generated method stub
    
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
