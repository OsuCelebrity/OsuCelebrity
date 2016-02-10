package me.reddev.osucelebrity.twitch;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.twitchapi.TwitchApiUser;

import javax.annotation.CheckForNull;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
@Getter
@ToString
public class TwitchUser {
  public TwitchUser(TwitchApiUser user) {
    super();
    this.user = user;
  }

  @PrimaryKey
  private TwitchApiUser user;

  @Setter
  @CheckForNull
  private OsuUser osuUser;
  
  @Setter
  @Index
  @CheckForNull
  private String linkString;
  
  @Setter
  private boolean trusted = true;
}
