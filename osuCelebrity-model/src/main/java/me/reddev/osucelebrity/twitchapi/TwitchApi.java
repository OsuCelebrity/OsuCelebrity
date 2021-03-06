package me.reddev.osucelebrity.twitchapi;

import me.reddev.osucelebrity.PassAndReturnNonnull;
import me.reddev.osucelebrity.core.QueuedPlayer;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.jdo.PersistenceManager;

@PassAndReturnNonnull
public interface TwitchApi {
  /**
   * Gets a list of moderators currently in a channel.
   * 
   * @return The list of moderator usernames in lowercase
   */
  List<String> getOnlineMods();

  /**
   * Determines whether a given user is a moderator of a channel.
   * 
   * @param username The username of the user
   * @return True if the user is a moderator
   */
  boolean isModerator(String username);

  /**
   * Retrieves the user from the API.
   * @param username username (all lower case).
   * @param maxAge if > 0, cached data up to this age can be returned.
   * @param returnCachedOnIoException if true, a cached data is used if a server exception
   *        occurrs during an update from the api
   * @return the user object.
   */
  TwitchApiUser getUser(PersistenceManager pm, String username, long maxAge,
      boolean returnCachedOnIoException);
  
  /**
   * Retrieves the link to the past broadcast that a play occurred in at the exact time when the
   * play started.
   * 
   * @param play any play.
   * @return the full URL. if no matching past broadcast can be found, null.
   */
  @CheckForNull
  URL getReplayLink(QueuedPlayer play) throws IOException;
}
