package me.reddev.osucelebrity.twitch.commands;

import me.reddev.osucelebrity.twitch.TwitchCommand;
import org.tillerino.osuApiModel.OsuApiUser;

/**
 * The user issued a command to enter someone into the queue of players to be spectated. 
 * @author Redback
 *
 */
public interface QueueUserTwitchCommand extends TwitchCommand {
  OsuApiUser getRequestUser();
}
