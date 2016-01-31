package me.reddev.osucelebrity.twitchapi;

import me.reddev.osucelebrity.PassAndReturnNonnull;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

@PassAndReturnNonnull
public interface TwitchApi {
  /**
   * Gets a list of moderators currently in a channel.
   * @return The list of moderator usernames in lowercase
   */
  List<String> getOnlineMods();
  
  /**
   * Determines whether a given user is a moderator of a channel.
   * @param username The username of the user
   * @return True if the user is a moderator
   */
  boolean isModerator(String username);

  List<Entry<String, List<String>>> getRoomMemberships() throws IOException;
}
