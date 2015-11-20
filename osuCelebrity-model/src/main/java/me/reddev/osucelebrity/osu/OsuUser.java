package me.reddev.osucelebrity.osu;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.reddev.osucelebrity.Priviledge;
import org.tillerino.osuApiModel.OsuApiUser;
import org.tillerino.osuApiModel.types.UserId;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(detachable = "true")
@Getter
@ToString
public class OsuUser {
  @PrimaryKey
  @UserId
  @Getter(onMethod = @__(@UserId))
  private int userId;

  @Index
  private String userName;

  private long downloaded;

  @Setter
  private Priviledge privilege = Priviledge.PLAYER;

  @Setter
  @Column(defaultValue = "true")
  private boolean allowsNotifications = true;
  
  @Setter
  @Column(defaultValue = "true")
  @Index
  private boolean allowsSpectating = true;

  /**
   * Creates a new user object copying all relevant data from the api user object.
   * 
   * @param user downloaded data.
   * @param downloaded current time in millis.
   */
  public OsuUser(OsuApiUser user, long downloaded) {
    this.userId = user.getUserId();
    update(user, downloaded);
  }

  /**
   * updates this object with the downloaded data.
   * 
   * @param user downloaded data
   * @param downloaded current time
   */
  public void update(OsuApiUser user, long downloaded) {
    this.setUserName(user.getUserName());
    this.setDownloaded(downloaded);
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

  void setDownloaded(long downloaded) {
    this.downloaded = downloaded;
  }

  void setUserName(String userName) {
    this.userName = userName;
  }
}
