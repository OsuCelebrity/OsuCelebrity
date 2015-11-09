package me.reddev.osucelebrity;

import javax.jdo.PersistenceManager;

public interface Command {
  PersistenceManager getPersistenceManager();
}
