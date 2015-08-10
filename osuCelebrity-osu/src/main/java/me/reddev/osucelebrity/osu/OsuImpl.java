package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.osuapi.OsuApiSettings;

import org.tillerino.osuApiModel.OsuApiUser;

import javax.annotation.CheckForNull;

public class OsuImpl implements Osu {
  final OsuApiSettings apiSettings;
  final OsuIrcSettings ircSettings;
  
  /**
   * constructs a new Osu instance.
   */
  public OsuImpl(OsuApiSettings apiSettings, OsuIrcSettings ircSettings) {
    super();
    this.apiSettings = apiSettings;
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
      bot = new OsuIrcBot(ircSettings, apiSettings, this);
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
