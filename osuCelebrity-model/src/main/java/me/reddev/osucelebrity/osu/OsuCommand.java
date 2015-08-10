package me.reddev.osucelebrity.osu;

import org.tillerino.osuApiModel.OsuApiUser;

public interface OsuCommand {
  Osu getOsu();

  /**
   * Provides the user who issued the command.
   * 
   * @return user object
   */
  OsuApiUser getUser();
}
