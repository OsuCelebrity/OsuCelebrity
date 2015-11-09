package me.reddev.osucelebrity.osu.commands;

import me.reddev.osucelebrity.osu.AbstractOsuCommand;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuUser;

import javax.jdo.PersistenceManager;

public class QueueSelfOsuCommandImpl extends AbstractOsuCommand implements QueueSelfOsuCommand {

  public QueueSelfOsuCommandImpl(Osu osu, OsuUser user, PersistenceManager persistenceManager) {
    super(osu, user, persistenceManager);
  }
  
  @Override
  public OsuUser getRequestedUser() {
    return getUser();
  }
}
