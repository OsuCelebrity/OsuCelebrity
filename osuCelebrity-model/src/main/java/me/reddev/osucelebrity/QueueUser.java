package me.reddev.osucelebrity;

import lombok.Getter;
import org.tillerino.osuApiModel.OsuApiUser;

/**
 * A user who is in the spectating queue.
 * @author Redback
 *
 */
public class QueueUser {
  @Getter
  OsuApiUser queuedPlayer;
  
  @Getter
  QueueSource queueSource;
  
  public QueueUser(OsuApiUser queuedUser, QueueSource source) {
    this.queuedPlayer = queuedUser;
    this.queueSource = source;
  }
  
  @Override
  public boolean equals(Object other) {
    //Matches QueueUser objects as well as OsuApiUser objects
    if (other instanceof QueueUser) {
      QueueUser otherUser = (QueueUser) other;
      return otherUser.queuedPlayer == this.queuedPlayer 
          && otherUser.queueSource == this.queueSource;
    } else if (other instanceof OsuApiUser) {
      OsuApiUser otherUser = (OsuApiUser) other;
      return otherUser == this.queuedPlayer;
    }
    return false;
  }
  
  public enum QueueSource {
    TWITCH, OSU
  }
}