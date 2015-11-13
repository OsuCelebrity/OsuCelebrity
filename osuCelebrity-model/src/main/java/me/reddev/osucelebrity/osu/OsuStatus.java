package me.reddev.osucelebrity.osu;

import lombok.Value;

@Value
public class OsuStatus {
  public enum Type {
    WATCHING,
    PLAYING,
  }
  
  Type type;
  
  String detail;
}
