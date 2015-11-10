package me.reddev.osucelebrity;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.Spectator;

/**
 * Simple class for user priviledge levels.
 */
@RequiredArgsConstructor
public enum Priviledge {
  ADMIN(true, true),
  MOD(false, true),
  PLAYER(false, false);
  
  /**
   * Can raise people's level to mod.
   */
  public final boolean canMod;
  /**
   * Can skip a player (see {@link Spectator#advance(javax.jdo.PersistenceManager)}.
   */
  public final boolean canSkip;
}
