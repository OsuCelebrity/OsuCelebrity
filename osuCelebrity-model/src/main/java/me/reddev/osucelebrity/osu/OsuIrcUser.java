package me.reddev.osucelebrity.osu;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.CheckForNull;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
@Data
@AllArgsConstructor
public class OsuIrcUser {
  @PrimaryKey
  String ircName;

  @CheckForNull
  OsuUser user;
  
  long resolved;
}
