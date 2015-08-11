package me.reddev.osucelebrity.twitch.commands;

import me.reddev.osucelebrity.twitch.AbstractTwitchCommand;
import me.reddev.osucelebrity.twitch.Twitch;

public class NextUserTwitchCommandImpl extends AbstractTwitchCommand implements
    NextUserTwitchCommand {

  public NextUserTwitchCommandImpl(Twitch twitch, String user) {
    super(twitch, user);
  }
}
