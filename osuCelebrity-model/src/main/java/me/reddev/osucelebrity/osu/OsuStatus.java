package me.reddev.osucelebrity.osu;

import lombok.EqualsAndHashCode;
import lombok.Value;
import me.reddev.osucelebrity.PassAndReturnNonnull;

import javax.annotation.CheckForNull;

@PassAndReturnNonnull
@Value
@EqualsAndHashCode(doNotUseGetters = true)
public class OsuStatus {
  public enum Type {
    /** Osu client is not open. */
    CLOSED,
    /** Osu client is open, but not connected to a spectatee. */
    IDLE,
    /** Osu client is "watching" a player, but we're not seeing any gameplay. */
    WATCHING,
    /** We're seeing gameplay. */
    PLAYING,
    /** Status unrecognized. */
    UNKNOWN,
  }

  Type type;

  @CheckForNull
  String detail;

  /**
   * @param type main status type.
   * @param detail may be null.
   */
  public OsuStatus(Type type, @CheckForNull String detail) {
    super();
    this.type = type;
    this.detail = detail;
  }
}
