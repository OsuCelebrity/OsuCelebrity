package me.reddev.osucelebrity.core;

import lombok.Data;
import me.reddev.osucelebrity.osu.OsuUser;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * A user who is in the spectating queue.
 * 
 * @author Redback
 */
@Data
@PersistenceCapable
public class QueuedPlayer {
  public static final int DONE = -1;
  public static final int SPECTATING = 0;
  public static final int NEXT = 1;
  public static final int QUEUED = 2;

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
  long id;

  OsuUser player;

  QueueSource queueSource;

  long queuedAt;

  long startedAt;

  long stoppingAt;
  
  @Column(defaultValue = "0")
  long lastRemainingTimeUpdate;

  @Index
  int state = 2;
  
  /**
   * Whether or not to notify a player of their upcoming turn via the osu chat.
   */
  @Column(defaultValue = "false")
  boolean notify = false;

  /**
   * When the player received their heads-up that they're on in a couple of seconds.
   */
  @Column(defaultValue = "-1")
  long notifiedAt = Long.MAX_VALUE;
  
  /**
   * Creates a new queued player object. This alone won't enqueue this player.
   * 
   * @param queuedPlayer the queued player's user object.
   * @param queueSource the user interface via which this player was enqued.
   * @param queuedAt current time.
   */
  public QueuedPlayer(OsuUser queuedPlayer, QueueSource queueSource, long queuedAt) {
    super();
    this.player = queuedPlayer;
    this.queueSource = queueSource;
    this.queuedAt = queuedAt;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof QueuedPlayer) {
      QueuedPlayer otherUser = (QueuedPlayer) other;
      return otherUser.player.getUserId() == player.getUserId();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return player != null ? player.getUserId() : 0;
  }

  public enum QueueSource {
    TWITCH, OSU, AUTO
  }
}
