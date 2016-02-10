package me.reddev.osucelebrity.twitch;

import me.reddev.osucelebrity.PassAndReturnNonnull;
import me.reddev.osucelebrity.core.SkipReason;
import me.reddev.osucelebrity.osu.OsuUser;

import java.io.IOException;

import javax.jdo.PersistenceManager;

@PassAndReturnNonnull
public interface Twitch {
  public void whisperUser(String nick, String message);

  public void announcePlayerSkipped(SkipReason reason, OsuUser player);

  /**
   * Retrieves a twitch user from the database or creates a new one.
   * 
   * @param username username (all lower case).
   * @param maxAge if > 0, cached data up to this age is allowed for the nested api user object.
   * @return the user object.
   */
  TwitchUser getUser(PersistenceManager pm, String username, long maxAge) throws IOException;
}
