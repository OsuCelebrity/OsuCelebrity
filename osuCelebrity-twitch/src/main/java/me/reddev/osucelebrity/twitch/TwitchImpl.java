package me.reddev.osucelebrity.twitch;

import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osuapi.OsuApi;

import javax.annotation.CheckForNull;

@Slf4j
public class TwitchImpl implements Twitch {
  final OsuApi osuApi;
  final TwitchIrcSettings ircSettings;
  final TwitchApiSettings apiSettings;
  
  @CheckForNull
  TwitchIrcBot bot;
  
  /**
   * Constructs a new Twitch instance.
   */
  public TwitchImpl(OsuApi twitchDownloader, TwitchIrcSettings ircSettings, 
      TwitchApiSettings apiSettings) {
    this.osuApi = twitchDownloader;
    this.ircSettings = ircSettings;
    this.apiSettings = apiSettings;
  }
  
  /**
   * Retrieves the irc bot instance.
   * @return Might not have been started.
   */
  public TwitchIrcBot getBot() {
    if (bot == null) {
      bot = new TwitchIrcBot(ircSettings, osuApi, this);
    }
    return bot;
  }
  
  @Override
  public void registerCommandHandler(TwitchCommandHandler handler) {
    getBot().dispatcher.addHandler(handler);
  }

  @Override
  public void sendMessageToChannel(String message) {
    getBot().sendMessage(message);
  }

  @Override
  public void whisperUser(String nick, String message) {
    // TODO Auto-generated method stub

  }

}
