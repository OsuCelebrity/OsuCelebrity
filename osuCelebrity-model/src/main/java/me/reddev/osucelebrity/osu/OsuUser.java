package me.reddev.osucelebrity.osu;

import lombok.Getter;

import org.tillerino.osuApiModel.OsuApiUser;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(detachable = "true")
@Getter
public class OsuUser {
  @PrimaryKey
  int userId;

  @Index
  String userName;

  private long downloaded;

  /**
   * Creates a new user object copying all relevant data from the api user object.
   * 
   * @param user downloaded data.
   * @param downloaded current time in millis.
   */
  public OsuUser(OsuApiUser user, long downloaded) {
    update(user);
    this.setDownloaded(downloaded);
  }

  /**
   * updates this object with the downloaded data.
   * 
   * @param user downloaded data
   */
  public void update(OsuApiUser user) {
    this.userId = user.getUserId();
    this.userName = user.getUserName();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OsuUser) {
      return ((OsuUser) obj).userId == userId;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return userId;
  }

  public void setDownloaded(long downloaded) {
    this.downloaded = downloaded;
  }
}
