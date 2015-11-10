package me.reddev.osucelebrity.core;

import javax.jdo.PersistenceManager;

public interface Spectator {
  /**
   * Add the given player to the queue.
   * 
   * @param the current request's persistence manager
   * @param user a new object (not persistent).
   * @return true if the player was added to the queue. Currently the only reason why a player
   *         cannot be added to the queue is that they are already in the queue. This might change
   *         in the future.
   */
  EnqueueResult enqueue(PersistenceManager pm, QueuedPlayer user);

  /**
   * Stops spectating the current player and advances to the next player.
   * 
   * @param pm the current request's persistence manager
   * @return true if the current player was skipped.
   */
  boolean advance(PersistenceManager pm);
}
