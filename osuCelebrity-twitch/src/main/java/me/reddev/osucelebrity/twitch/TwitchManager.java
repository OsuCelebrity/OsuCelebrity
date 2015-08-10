package me.reddev.osucelebrity.twitch;

import me.reddev.osucelebrity.osuapi.OsuApiSettings;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.Responses;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.OsuApiUser;

@RequiredArgsConstructor
public class TwitchManager {
  private TwitchIrcBot ircBot;
  private TwitchRequest requests;

  private final TwitchIrcSettings ircSettings;
  private final OsuApiSettings osuApiSettings;

  /**
   * Starts the twitch manager.
   */
  public void start() {
    this.requests = new TwitchRequest();
    // Create a Twitch bot
    this.ircBot = new TwitchIrcBot(ircSettings, this, 
        new Downloader(osuApiSettings.getOsuApiKey()));
    this.ircBot.start();
  }

  /**
   * Adds a new user to the request list.
   * 
   * @param user The user to request
   */
  public void addRequest(OsuApiUser user) {
    getRequests().addRequest(user);
    ircBot.sendMessage(String.format(Responses.ADDED_TO_QUEUE, user.getUserName()));
    ircBot.sendMessage(String.format(Responses.CURRENT_QUEUE, getRequests().getRequestCount()));
  }

  /**
   * Notifies the stream about who the current player is.
   * 
   * @param user The username of the player being spectated
   */
  public void notifySpectate(String user) {
    ircBot.sendMessage(String.format(Responses.CURRENT_PLAYER, user));
  }

  /**
   * @return The request object connected to the Twitch chat.
   */
  public TwitchRequest getRequests() {
    if (requests == null) {
      return (requests = new TwitchRequest());
    } else {
      return requests;
    }
  }

  /**
   * @return The Twitch IRC bot connected to the Twitch chat.
   */
  public TwitchIrcBot getIrcBot() {
    return ircBot;
  }
}
