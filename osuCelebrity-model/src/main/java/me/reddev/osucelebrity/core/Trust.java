package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.twitch.TwitchUser;

import java.io.IOException;

import javax.jdo.PersistenceManager;

public interface Trust {
  void checkTrust(PersistenceManager pm, TwitchUser user) throws IOException, UserException;
}
