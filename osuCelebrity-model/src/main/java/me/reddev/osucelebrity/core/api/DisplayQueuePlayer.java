package me.reddev.osucelebrity.core.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DisplayQueuePlayer {
  public String name;
  
  public String timeInQueue;
  
  public int votes;
}