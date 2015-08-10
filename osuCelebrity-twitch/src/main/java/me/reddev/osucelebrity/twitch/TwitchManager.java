package me.reddev.osucelebrity.twitch;

import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.OsuApiUser;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.Responses;
import me.reddev.osucelebrity.osu.OsuApiSettings;

@RequiredArgsConstructor
public class TwitchManager {
  private TwitchIrcBot _ircBot;
  private TwitchRequest _requests;

  private final TwitchIrcSettings _ircSettings;
  private final OsuApiSettings _osuApiSettings;

  public void start() {
    _requests = new TwitchRequest();
    // Create a Twitch bot
    _ircBot = new TwitchIrcBot(_ircSettings, this, new Downloader(_osuApiSettings.getOsuApiKey()));
    _ircBot.start();
  }

  /**
   * Adds a new user to the request list
   * 
   * @param user The user to request
   */
  public void addRequest(OsuApiUser user) {
    getRequests().AddRequest(user);
    _ircBot.sendMessage(String.format(Responses.ADDED_TO_QUEUE, user.getUserName()));
    _ircBot.sendMessage(String.format(Responses.CURRENT_QUEUE, getRequests().getRequestCount()));
  }

  /**
   * Notifies the stream about who the current player is
   * 
   * @param user The username of the player being spectated
   */
  public void notifySpectate(String user) {
    _ircBot.sendMessage(String.format(Responses.CURRENT_PLAYER, user));
  }

  /**
   * @return The request object connected to the Twitch chat
   */
  public TwitchRequest getRequests() {
    if (_requests == null)
      return (_requests = new TwitchRequest());
    else
      return _requests;
  }

  /**
   * @return The Twitch IRC bot connected to the Twitch chat
   */
  public TwitchIrcBot getIRCBot() {
    return _ircBot;
  }
}
