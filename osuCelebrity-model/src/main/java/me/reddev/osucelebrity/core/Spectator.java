package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.PassAndReturnNonnull;
import me.reddev.osucelebrity.osu.OsuUser;

import javax.annotation.CheckForNull;
import javax.jdo.PersistenceManager;

@PassAndReturnNonnull
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
   * @param expected only advances if this player is currently being spectated.
   * @return true if the current player was skipped.
   */
  boolean advanceConditional(PersistenceManager pm, OsuUser expected);

  /**
   * Gets all information about the player currently being spectated.
   * 
   * @param pm the current request's persistence manager
   * @return null, if there is no current player.
   */
  @CheckForNull
  QueuedPlayer getCurrentPlayer(PersistenceManager pm);
  
  /**
   * Gets all information about the player being spectated next.
   * 
   * @param pm the current request's persistence manager
   * @return null, if the next player has not been determined yet.
   */
  @CheckForNull
  QueuedPlayer getNextPlayer(PersistenceManager pm);
  
  /**
   * Votes for the current player.
   * 
   * @param pm the current request's persistence manager
   * @param twitchIrcNick the voting player's twitch irc nickname
   * @param voteType up/down
   * @return true if the vote will count, false otherwise.
   */
  boolean vote(PersistenceManager pm, String twitchIrcNick, VoteType voteType);
  
  /**
   * Removes the given player from the queue.
   * 
   * @param pm the current request's persistence manager
   * @param player the player to be deleted from the queue. does not need to be in the queue.
   */
  void removeFromQueue(PersistenceManager pm, OsuUser player);
}
