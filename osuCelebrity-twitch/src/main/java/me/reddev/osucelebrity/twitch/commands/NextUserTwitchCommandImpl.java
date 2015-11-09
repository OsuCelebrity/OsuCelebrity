package me.reddev.osucelebrity.twitch.commands;

import me.reddev.osucelebrity.twitch.AbstractTwitchCommand;
import me.reddev.osucelebrity.twitch.Twitch;

import javax.jdo.PersistenceManager;

public class NextUserTwitchCommandImpl extends AbstractTwitchCommand implements
    NextUserTwitchCommand {

  public NextUserTwitchCommandImpl(Twitch twitch, String user,
      PersistenceManager persistenceManager) {
    super(twitch, user, persistenceManager);
  }
}
