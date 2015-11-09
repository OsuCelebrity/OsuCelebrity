package me.reddev.osucelebrity.osu;

import lombok.Data;

import javax.jdo.PersistenceManager;

@Data
public abstract class AbstractOsuCommand implements OsuCommand {
  private final Osu osu;
  private final OsuUser user;
  private final PersistenceManager persistenceManager;
}
