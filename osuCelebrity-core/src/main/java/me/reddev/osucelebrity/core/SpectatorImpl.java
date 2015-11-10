package me.reddev.osucelebrity.core;

import static me.reddev.osucelebrity.core.QVote.vote;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.twitch.Twitch;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * Controller for what's shown on the screen. Currently all methods except run() are synchronized.
 * This has the potential to block other threads.
 * 
 * @author Tillerino
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SpectatorImpl implements Spectator, Runnable {
  final Twitch twitch;

  final Clock clock;

  final Osu osu;

  final CoreSettings settings;

  final PersistenceManagerFactory pmf;

  boolean run = true;

  Thread runThread = null;

  @Override
  public void run() {
    try {
      for (; run;) {
        PersistenceManager pm = pmf.getPersistenceManager();
        try {
          long sleepUntil = loop(pm);
          runThread = Thread.currentThread();
          clock.sleepUntil(sleepUntil);
        } catch (InterruptedException e) {
          // time to wake up and work!
        } finally {
          pm.close();
        }
      }
    } catch (Exception e) {
      log.error("Exception in spectator", e);
    }
  }

  /**
   * Wakes the spectator from sleep.
   */
  public void wake() {
    if (runThread != null) {
      runThread.interrupt();
    }
  }

  synchronized long loop(PersistenceManager pm) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    if (!queue.currentlySpectating().isPresent() || queue.spectatingUntil() < clock.getTime()) {
      advance(queue);
    } else if (queue.spectatingUntil() < clock.getTime() + settings.getNextPlayerNotifyTime()
        && !queue.spectatingNext().isPresent()) {
      lockNext(queue);
    } else {
      updateRemainingTime(pm, queue.currentlySpectating().get());
    }
    long until = queue.currentlySpectating().isPresent() ? queue.spectatingUntil() : 0;
    return Math.min(clock.getTime() + 100, until > clock.getTime() ? until : clock.getTime() + 100);
  }

  private void updateRemainingTime(PersistenceManager pm, QueuedPlayer queuedPlayer) {
    double approval = getApproval(pm, queuedPlayer);
    long time = clock.getTime();
    if (approval > .5) {
      queuedPlayer.setStoppingAt(time + queuedPlayer.getStoppingAt()
          - queuedPlayer.getLastRemainingTimeUpdate());
    }

    queuedPlayer.setLastRemainingTimeUpdate(time);
  }

  double getApproval(PersistenceManager pm, QueuedPlayer queuedPlayer) {
    double approval;
    try (JDOQuery<Vote> query = new JDOQuery<>(pm)) {
      query
          .select(vote)
          .from(vote)
          .where(vote.referece.eq(queuedPlayer),
              vote.voteTime.goe(clock.getTime() - settings.getVoteWindow()))
          .orderBy(vote.voteTime.asc());

      Map<String, Vote> votes = new HashMap<>();
      query.fetch().stream().forEach(vote -> votes.put(vote.getTwitchUser(), vote));
      long upVotes =
          votes.values().stream().filter(x -> x.getVoteType().equals(VoteType.UP)).count();
      approval = upVotes / (double) votes.size();
    }
    return approval;
  }

  synchronized void lockNext(PlayerQueue queue) {
    Optional<QueuedPlayer> spectatingNext = queue.poll();
    if (spectatingNext.isPresent()) {
      spectatingNext.get().setState(QueuedPlayer.NEXT);
      // TODO send 10 second warning
    }
  }

  /**
   * Adds a player to the queue.
   * 
   * @return true if the player was added, false if they are already in the queue or currently being
   *         spectated.
   */
  @Override
  public synchronized EnqueueResult enqueue(PersistenceManager pm, QueuedPlayer user) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    if (queue.contains(user)) {
      return EnqueueResult.FAILURE;
    }
    pm.makePersistent(user);
    log.info("Queued " + user.getPlayer().getUserName());
    // wake spectator in the case that the queue was empty.
    wake();
    return EnqueueResult.SUCCESS;
  }

  /**
   * Advances to the next player in queue.
   * 
   * @return True if successful, false if there is no next queued player.
   */
  @Override
  public synchronized boolean advance(PersistenceManager pm) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    return advance(queue);
  }

  boolean advance(PlayerQueue queue) {
    Optional<QueuedPlayer> next = queue.spectatingNext();
    if (!next.isPresent()) {
      next = queue.poll();
    }
    if (!next.isPresent()) {
      return false;
    }
    // TODO say good-bye to the player currently being spectated
    startSpectating(queue, next.get());
    return true;
  }

  private synchronized void startSpectating(PlayerQueue queue, QueuedPlayer next) {
    Optional<QueuedPlayer> spectating = queue.currentlySpectating();
    if (spectating.isPresent()) {
      spectating.get().setState(QueuedPlayer.DONE);
    }
    long time = clock.getTime();
    next.setState(QueuedPlayer.SPECTATING);
    next.setStartedAt(time);
    next.setLastRemainingTimeUpdate(time);
    next.setStoppingAt(next.getStartedAt() + settings.getDefaultSpecDuration());
    OsuUser user = next.getPlayer();
    // TODO enable notification after alpha
    // osu.notifyStarting(user);
    osu.startSpectate(user);
  }

  @Override
  public QueuedPlayer getCurrentPlayer(PersistenceManager pm) {
    return PlayerQueue.loadQueue(pm).currentlySpectating().orElse(null);
  }

  @Override
  public boolean vote(PersistenceManager pm, String twitchIrcNick, VoteType voteType) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    Optional<QueuedPlayer> currentlySpectating = queue.currentlySpectating();
    if (!currentlySpectating.isPresent()) {
      return false;
    }
    if (queue.spectatingNext().isPresent()) {
      return false;
    }
    Vote vote = new Vote();
    vote.setReferece(currentlySpectating.get());
    vote.setVoteType(voteType);
    vote.setVoteTime(clock.getTime());
    vote.setTwitchUser(twitchIrcNick);
    pm.makePersistent(vote);
    return true;
  }
}
