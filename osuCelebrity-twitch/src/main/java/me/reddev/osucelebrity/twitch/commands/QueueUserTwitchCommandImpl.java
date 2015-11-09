package me.reddev.osucelebrity.twitch.commands;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.twitch.AbstractTwitchCommand;
import me.reddev.osucelebrity.twitch.Twitch;

import javax.jdo.PersistenceManager;

@EqualsAndHashCode(callSuper = true)
public class QueueUserTwitchCommandImpl extends AbstractTwitchCommand implements
    QueueUserTwitchCommand {

  @Getter
  OsuUser requestedUser;

  /**
   * Creates a command to queue a requested user.
   * 
   * @param requestUser The osu! user to add to the queue
   */
  public QueueUserTwitchCommandImpl(Twitch twitch, String user, OsuUser requestUser,
      PersistenceManager pm) {
    super(twitch, user, pm);

    this.requestedUser = requestUser;
  }
}
