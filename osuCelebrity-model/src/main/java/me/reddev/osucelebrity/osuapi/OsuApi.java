package me.reddev.osucelebrity.osuapi;

import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osu.OsuUser;

import org.tillerino.osuApiModel.types.GameMode;
import org.tillerino.osuApiModel.types.UserId;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.jdo.PersistenceManager;

public interface OsuApi {
  /**
   * Get a user object from the osu api.
   * 
   * @param userid the user's id
   * @param gameMode game mode for rank, pp, ...
   * @param pm the requests's persistence manager
   * @param maxAge maximum age of the returned object. If there is a cached object which is younger
   *        than maximum age or maxAge is <= 0, it may be returned.
   * @return null if the user does not exist
   */
  @CheckForNull
  OsuUser getUser(@UserId int userid, @GameMode int gameMode, PersistenceManager pm, long maxAge)
      throws IOException;

  /**
   * Get a user object from the osu api.
   * 
   * @param userName the user's name
   * @param gameMode game mode for rank, pp, ...
   * @param pm the requests's persistence manager
   * @param maxAge maximum age of the returned object. If there is a cached object which is younger
   *        than maximum age or maxAge is <= 0, it may be returned.
   * @return null if the user does not exist
   */
  @CheckForNull
  OsuUser getUser(String userName, @GameMode int gameMode, PersistenceManager pm, long maxAge)
      throws IOException;

  /**
   * Get an irc user object from the osu api.
   * 
   * @param ircUserName the user's name on the osu irc chat
   * @param gameMode game mode for rank, pp, ...
   * @param pm the requests's persistence manager
   * @param maxAge maximum age of the returned object. If there is a cached object which is younger
   *        than maximum age or maxAge is <= 0, it may be returned.
   * @return null if the user does not exist
   */
  @CheckForNull
  OsuIrcUser getIrcUser(String ircUserName, @GameMode int gameMode, PersistenceManager pm,
      long maxAge) throws IOException;
}
