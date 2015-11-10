package me.reddev.osucelebrity.core;

import lombok.Data;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
@Data
public class Vote {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
  int id;

  @Index
  QueuedPlayer referece;

  long voteTime;

  String twitchUser;

  VoteType voteType;
}
