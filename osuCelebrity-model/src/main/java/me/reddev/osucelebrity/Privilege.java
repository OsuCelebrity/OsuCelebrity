package me.reddev.osucelebrity;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.Spectator;

/**
 * Simple class for user priviledge levels.
 */
@RequiredArgsConstructor
public enum Privilege {
  ADMIN(true, true, true),
  MOD(false, true, true),
  PLAYER(false, false, false);
  
  /**
   * Can raise people's level to mod.
   */
  public final boolean canMod;
  /**
   * Can skip a player (see {@link Spectator#advance(javax.jdo.PersistenceManager)}.
   */
  public final boolean canSkip;
  /**
   * Can skip a player (see {@link Spectator#advance(javax.jdo.PersistenceManager)}.
   */
  public final boolean canRestartClient;
}
