package me.reddev.osucelebrity.osu;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.reddev.osucelebrity.PassAndReturnNonnull;

@PassAndReturnNonnull
@Value
@EqualsAndHashCode(doNotUseGetters = true)
public class OsuStatus {
  public enum Type {
    WATCHING, PLAYING,
  }

  Type type;

  String detail;
}
