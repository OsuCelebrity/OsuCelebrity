package me.reddev.osucelebrity.osu.commands;

import me.reddev.osucelebrity.core.QueueUserCommand;
import me.reddev.osucelebrity.osu.OsuCommand;

/**
 * The user issued a command to enter someone into the queue of players to be spectated. 
 * @author Redback
 *
 */
public interface QueueUserOsuCommand extends OsuCommand, QueueUserCommand {

}
