package me.reddev.osucelebrity.osuapi;

import org.tillerino.osuApiModel.OsuApiUser;
import org.tillerino.osuApiModel.types.GameMode;
import org.tillerino.osuApiModel.types.UserId;

import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.CheckForNull;

public interface OsuApi {
  /**
   * Get a user object from the osu api.
   * 
   * @param userid the user's id
   * @param gameMode game mode for rank, pp, ...
   * @param maxAge maximum age of the returned object. If there is a cached object which is younger
   *        than maximum age or maxAge is <= 0, it may be returned.
   * @return null if the user does not exist
   */
  @CheckForNull
  OsuApiUser getUser(@UserId int userid, @GameMode int gameMode, long maxAge) throws IOException,
      SQLException;

  /**
   * Get a user object from the osu api.
   * 
   * @param ircUserName the user's name in the irc chat
   * @param gameMode game mode for rank, pp, ...
   * @param maxAge maximum age of the returned object. If there is a cached object which is younger
   *        than maximum age or maxAge is <= 0, it may be returned.
   * @return null if the user does not exist
   */
  @CheckForNull
  OsuApiUser getUser(String ircUserName, @GameMode int gameMode, long maxAge) throws IOException,
      SQLException;
}
