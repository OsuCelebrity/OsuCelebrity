package me.reddev.osucelebrity.osu;

import me.reddev.osucelebrity.Command;

public interface OsuCommand extends Command {
  Osu getOsu();

  /**
   * Provides the user who issued the command.
   * 
   * @return user object
   */
  OsuUser getUser();
}
