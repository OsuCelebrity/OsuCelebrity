package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.PassAndReturnNonnull;
import me.reddev.osucelebrity.core.api.DisplayQueuePlayer;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osu.PlayerStatus;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.jdo.PersistenceManager;


@PassAndReturnNonnull
public interface Spectator {
  /**
   * Performs some checks and adds the given player to the queue.
   * 
   * @param pm the current request's persistence manager
   * @param user a new object (not persistent).
   * @param selfqueue if this is a self-queue some checks are skipped.
   * @return the result of the operation. See {@link EnqueueResult}
   */
  default EnqueueResult enqueue(PersistenceManager pm, QueuedPlayer user, boolean selfqueue)
      throws IOException {
    return enqueue(pm, user, selfqueue, null, true);
  }

  /**
   * Performs some checks and adds the given player to the queue.
   * 
   * @param pm the current request's persistence manager
   * @param user a new object (not persistent).
   * @param selfqueue if this is a self-queue some checks are skipped.
   * @param twitchUser the twitch user's name or null if queued from somewhere else
   * @param online if the target user is online.
   * @return the result of the operation. See {@link EnqueueResult}
   */
  EnqueueResult enqueue(PersistenceManager pm, QueuedPlayer user, boolean selfqueue,
      @CheckForNull String twitchUser, boolean online) throws IOException;

  /**
   * Stops spectating the current player and advances to the next player.
   * 
   * @param pm the current request's persistence manager
   * @param expected only advances if this player is currently being spectated.
   * @return true if the current player was skipped.
   */
  boolean advanceConditional(PersistenceManager pm, String expected);

  /**
   * Starts spectating a player immediately.
   * @param pm the current request's persistence manager.
   * @param ircUser the user to promote.
   * @return true if the player was successfully promoted.
   */
  boolean promote(PersistenceManager pm, OsuUser ircUser);

  /**
   * Puts a player to the front of the queue keeping the current and next player.
   * @param pm the current request's persistence manager.
   * @param ircUser the user to put ahead.
   * @return true if the operation was successful.
   */
  boolean boost(PersistenceManager pm, OsuUser ircUser);
  
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
  
  /**
   * Returns the current queue size.
   * @param pm the current request's persistence manager
   * @return does not include the current player
   */
  int getQueueSize(PersistenceManager pm);
  
  /**
   * Returns the position of a player in the queue.
   * @param pm the current request's persistence manager
   * @param player the player to get the position of
   * @return the position of the player in the queue or -1 if not in queue
   */
  int getQueuePosition(PersistenceManager pm, OsuUser player);

  /**
   * returns the current top of the queue excluding the current and the next player.
   * @param pm the current request's persistence manager
   * @return the full queue
   */
  List<DisplayQueuePlayer> getCurrentQueue(PersistenceManager pm);
  
  /**
   * Sends the user the statistics about their round, after their round is over.
   * @param pm the current request's persistence manager
   * @param player the player of the round
   */
  void sendEndStatistics(PersistenceManager pm, QueuedPlayer player);

  /**
   * Informs the spectator that a player is offline.
   * @param pm the current request's persistence manager
   * @param osuUser the offline player
   */
  void reportStatus(PersistenceManager pm, PlayerStatus status);

  /**
   * This attempts an enqueue operation and checks if the target user is online if this is
   * requested.
   * 
   * @param persistenceManager the request's persistence manager
   * @param queueRequest the queue request
   * @param requestingUser the requesting user as a unique string. used for voting.
   * @param log the requester's log.
   * @param reply positive responses to the user are sent here
   * @param replyNegative negative responses are sent here
   */
  void performEnqueue(PersistenceManager persistenceManager, QueuedPlayer queueRequest,
      String requestingUser, Logger log, Consumer<String> reply, Consumer<String> replyNegative)
      throws IOException;
  
  /**
   * Adds a filter for banned maps. If the current player plays a map which matches this filter,
   * they will be skipped.
   * 
   * @param persistenceManager the request's persistence manager
   * @param startsWith beatmaps with a title that starts with this string will be banned.
   */
  void addBannedMapFilter(PersistenceManager persistenceManager, String startsWith);

  /**
   * Returns the relevant votes for the current player.
   * @param pm the request's persistence manager
   * @param queuedPlayer the player who the votes are associated with.
   * @return each voter's last vote.
   */
  List<Vote> getVotes(PersistenceManager pm, QueuedPlayer queuedPlayer);

  /**
   * Extend the time of the current player.
   * @param pm the request's persistence manager
   * @param expectedUser only extends the time if this player is currently being spectated.
   * @return success
   */
  boolean extendConditional(PersistenceManager pm, String expectedUser);
}
