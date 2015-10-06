package me.reddev.osucelebrity.twitch;

public interface TwitchCommand {
  Twitch getTwitch();
  
  /**
   * Provides the username of the Twitch user who made the command.
   * @return The Twitch username.
   */
  String getUser();
}
