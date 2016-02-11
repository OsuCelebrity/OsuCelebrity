package me.reddev.osucelebrity.core;

import lombok.Data;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
@Data
public class QueueVote {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
  private long id;
  
  @Index
  private QueuedPlayer reference;
  
  private String twitchUser;

  public static final String TWITCH = "twitch:";
  public static final String OSU = "osu:";

  /**
   * Constructs a new vote for a queued player.
   * @param reference the queued player.
   * @param twitchUser the voting player.
   */
  public QueueVote(QueuedPlayer reference, String twitchUser) {
    super();
    this.reference = reference;
    this.twitchUser = twitchUser;
  }
}
