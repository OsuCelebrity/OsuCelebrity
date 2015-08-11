package me.reddev.osucelebrity.twitch.commands;

import lombok.Getter;
import me.reddev.osucelebrity.twitch.AbstractTwitchCommand;
import me.reddev.osucelebrity.twitch.Twitch;
import org.tillerino.osuApiModel.OsuApiUser;

public class QueueUserTwitchCommandImpl extends AbstractTwitchCommand 
    implements QueueUserTwitchCommand {

  @Getter
  OsuApiUser requestUser;
  
  /**
   * Creates a command to queue a requested user.
   * @param requestUser The osu! user to add to the queue
   */
  public QueueUserTwitchCommandImpl(Twitch twitch, String user, OsuApiUser requestUser) {
    super(twitch, user);
    
    this.requestUser = requestUser;
  }
}
