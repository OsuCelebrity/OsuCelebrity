package me.reddev.osucelebrity.twitch;

import me.reddev.osucelebrity.Command;

public interface TwitchCommand extends Command {
  Twitch getTwitch();
  
  /**
   * Provides the username of the Twitch user who made the command.
   * @return The Twitch username.
   */
  String getUser();
}
