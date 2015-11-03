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
      return otherUser.queuedPlayer.getUserId() == queuedPlayer.getUserId();
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return queuedPlayer != null ? queuedPlayer.getUserId() : 0;
  }
  
  public enum QueueSource {
    TWITCH, OSU
  }
}