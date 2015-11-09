package me.reddev.osucelebrity.osu.commands;

import lombok.Getter;
import me.reddev.osucelebrity.osu.AbstractOsuCommand;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuUser;

import javax.jdo.PersistenceManager;

public class QueueUserOsuCommandImpl extends AbstractOsuCommand implements QueueUserOsuCommand {

  @Getter
  OsuUser requestedUser;

  public QueueUserOsuCommandImpl(Osu osu, OsuUser user, OsuUser requestedUser,
      PersistenceManager persistenceManager) {
    super(osu, user, persistenceManager);
    this.requestedUser = requestedUser;
  }
}
