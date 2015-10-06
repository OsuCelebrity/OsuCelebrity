package me.reddev.osucelebrity.osu;

import lombok.Data;
import org.tillerino.osuApiModel.OsuApiUser;

@Data
public abstract class AbstractOsuCommand implements OsuCommand {
  private final Osu osu;
  private final OsuApiUser user;
}
