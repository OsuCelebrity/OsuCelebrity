package me.reddev.osucelebrity.twitch.commands;

import me.reddev.osucelebrity.core.QueueUserCommand;
import me.reddev.osucelebrity.twitch.TwitchCommand;

/**
 * The user issued a command to enter someone into the queue of players to be spectated. 
 * @author Redback
 *
 */
public interface QueueUserTwitchCommand extends TwitchCommand, QueueUserCommand {
}
