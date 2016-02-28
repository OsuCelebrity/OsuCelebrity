package me.reddev.osucelebrity.core.api;

import lombok.Data;
import me.reddev.osucelebrity.core.VoteType;

@Data
public class DisplayVote {
  public long id;
  
  long voteTime;

  String twitchUser;

  VoteType voteType;
  
  String command;
}