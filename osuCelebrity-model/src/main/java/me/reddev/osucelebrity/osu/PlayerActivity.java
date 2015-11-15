package me.reddev.osucelebrity.osu;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.reddev.osucelebrity.osuapi.ApiUser;
import org.tillerino.osuApiModel.OsuApiScore;

import java.util.List;
import java.util.OptionalLong;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@Getter
@AllArgsConstructor
@PersistenceCapable
public class PlayerActivity {
  @PrimaryKey
  private ApiUser user;

  @Setter(AccessLevel.PACKAGE)
  private long lastActivity;

  @Setter(AccessLevel.PACKAGE)
  private long lastChecked;

  /**
   * updates this object with the downloaded data.
   * 
   * @param scores downloaded data
   * @param downloaded current time
   */
  public void update(List<OsuApiScore> scores, long downloaded) {
    OptionalLong max = scores.stream().mapToLong(OsuApiScore::getDate).max();
    if (max.isPresent()) {
      setLastActivity(max.getAsLong());
    }
    setLastChecked(downloaded);
  }
}
