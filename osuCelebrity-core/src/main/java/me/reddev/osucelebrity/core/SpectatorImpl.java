package me.reddev.osucelebrity.core;

import static me.reddev.osucelebrity.core.QQueuedPlayer.queuedPlayer;
import static me.reddev.osucelebrity.core.QVote.vote;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;
import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;

import com.google.common.base.Objects;

import com.querydsl.core.Tuple;
import com.querydsl.jdo.JDOQuery;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.twitch.Twitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

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
    Optional<QueuedPlayer> current = queue.currentlySpectating();
    Optional<QueuedPlayer> next = lockNext(queue);
    long time = clock.getTime();

    if (current.isPresent() && next.isPresent()) {
      QueuedPlayer currentPlayer = current.get();
      QueuedPlayer nextPlayer = next.get();
      if (nextPlayer.isNotify() && nextPlayer.getNotifiedAt() == Long.MAX_VALUE
          && clock.getTime() >= currentPlayer.getStoppingAt()
          - settings.getNextPlayerNotifyTime()) {
        osu.notifySoon(nextPlayer.getPlayer());
        nextPlayer.setNotifiedAt(clock.getTime());
      }
    }
    if (current.isPresent()) {
      SkipReason shouldSkip = status.shouldSkip();
      if (shouldSkip != null) {
        if (advance(pm, queue)) {
          twitch.announcePlayerSkipped(shouldSkip, current.get().getPlayer());
        }
      } else {
        if (queue.spectatingUntil() <= time) {
          if (next.isPresent()) {
            advance(pm, queue);
          } else {
            if (queue.spectatingUntil() <= time
                - (settings.getAutoSpecTime() - settings.getDefaultSpecDuration())) {
              advance(pm, queue);
            }
          }
        }
      }
    } else {
      advance(pm, queue);
    }

    updateRemainingTime(pm, queue);
    long until = queue.currentlySpectating().isPresent() ? queue.spectatingUntil() : 0;
    return Math.min(time + 100, until > time ? until : time + 100);
  }

  class Status {
    long lastStatusChange = -1;
    OsuStatus lastStatus;

    @SuppressFBWarnings(value = "NP",
        justification = "lastStatus = currentStatus raises a weird bug")
    SkipReason shouldSkip() {
      OsuStatus currentStatus = osu.getClientStatus();
      try {
        if (lastStatusChange == -1 || !Objects.equal(currentStatus, lastStatus)) {
          lastStatusChange = clock.getTime();
          return null;
        }
        if (currentStatus == null
            && lastStatusChange <= clock.getTime() - settings.getOfflineTimeout()) {
          return SkipReason.OFFLINE;
        }
        if (currentStatus != null && currentStatus.getType() == Type.WATCHING
            && lastStatusChange <= clock.getTime() - settings.getIdleTimeout()) {
          return SkipReason.IDLE;
        }
        return null;
      } finally {
        lastStatus = currentStatus;
      }
    }
  }

  Status status = new Status();

  private void updateRemainingTime(PersistenceManager pm, PlayerQueue queue) {
    Optional<QueuedPlayer> currentlySpectating = queue.currentlySpectating();
    if (!currentlySpectating.isPresent()) {
      return;
    }
    QueuedPlayer current = currentlySpectating.get();

    if (queue.spectatingNext().isPresent()) {
      current.setLastRemainingTimeUpdate(clock.getTime());
      return;
    }

    double approval = getApproval(pm, current);
    long time = clock.getTime();
    if (approval > .7) {
      long lastRemainingTime = current.getStoppingAt() - current.getLastRemainingTimeUpdate();
      long newRemainingTime =
          Math.min(settings.getDefaultSpecDuration(),
              lastRemainingTime + time - current.getLastRemainingTimeUpdate());
      current.setStoppingAt(time + newRemainingTime);
    } else if (approval > .4) {
      current.setStoppingAt(time + current.getStoppingAt() - current.getLastRemainingTimeUpdate());
    }

    current.setLastRemainingTimeUpdate(time);
  }

  double getApproval(PersistenceManager pm, QueuedPlayer queuedPlayer) {
    double approval;
    try (JDOQuery<Vote> query = new JDOQuery<>(pm)) {
      query
          .select(vote)
          .from(vote)
          .where(vote.reference.eq(queuedPlayer),
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

  synchronized Optional<QueuedPlayer> lockNext(PlayerQueue queue) {
    Optional<QueuedPlayer> spectatingNext = queue.spectatingNext();
    if (!spectatingNext.isPresent()) {
      spectatingNext = queue.poll();
      if (spectatingNext.isPresent()) {
        spectatingNext.get().setState(QueuedPlayer.NEXT);
      }
    }
    return spectatingNext;
  }

  /**
   * Adds a player to the queue.
   * 
   * @return true if the player was added, false if they are already in the queue or currently being
   *         spectated.
   */
  @Override
  public synchronized EnqueueResult enqueue(PersistenceManager pm, QueuedPlayer user) {
    if (!user.getPlayer().isAllowsSpectating()) {
      return EnqueueResult.DENIED;
    }
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

  @Override
  public synchronized boolean advanceConditional(PersistenceManager pm, OsuUser expectedUser) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    Optional<QueuedPlayer> currentUser = queue.currentlySpectating();
    if (!currentUser.isPresent()) {
      return false;
    }
    if (!currentUser.get().getPlayer().equals(expectedUser)) {
      return false;
    }
    return advance(pm, queue);
  }
  
  @Override
  public synchronized boolean promote(PersistenceManager pm, OsuUser ircUser) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    Optional<QueuedPlayer> nextUser = queue.spectatingNext();
    QueuedPlayer queueRequest = new QueuedPlayer(ircUser, QueueSource.TWITCH, clock.getTime());
    
    //Don't force spectate a denied player
    EnqueueResult enqueueResult = enqueue(pm, queueRequest);
    if (enqueueResult == EnqueueResult.DENIED) {
      return false;
    }
    if (enqueueResult == EnqueueResult.FAILURE || enqueueResult == EnqueueResult.VOTED) {
      QueuedPlayer original = queueRequest;
      queueRequest = queue.queue.stream().filter(x -> x.equals(original)).findFirst().get();
    }
    
    //Revert the next player
    if (nextUser.isPresent()) {
      nextUser.get().setState(QueuedPlayer.QUEUED);
    }
    
    queueRequest.setState(QueuedPlayer.NEXT);
    queue = PlayerQueue.loadQueue(pm);
    advance(pm, queue);
    
    return true;
  }

  boolean advance(PersistenceManager pm, PlayerQueue queue) {
    Optional<QueuedPlayer> next = queue.spectatingNext();
    if (!next.isPresent()) {
      next = queue.poll();
    }
    if (!next.isPresent()) {
      next = pickAutoPlayer(pm, queue.currentlySpectating().orElse(null));
    }
    if (!next.isPresent()) {
      return false;
    }
    // TODO say good-bye to the player currently being spectated
    startSpectating(queue, next.get());
    status = new Status();
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
    if (next.isNotify()) {
      osu.notifyStarting(user);
    }
    osu.startSpectate(user);
  }

  @Override
  public synchronized QueuedPlayer getCurrentPlayer(PersistenceManager pm) {
    return PlayerQueue.loadQueue(pm).currentlySpectating().orElse(null);
  }

  @Override
  public synchronized boolean vote(PersistenceManager pm, String twitchIrcNick, VoteType voteType) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    Optional<QueuedPlayer> currentlySpectating = queue.currentlySpectating();
    if (!currentlySpectating.isPresent()) {
      return false;
    }
    Optional<QueuedPlayer> next = queue.spectatingNext();
    if (next.isPresent() && next.get().getNotifiedAt() < Long.MAX_VALUE) {
      return false;
    }
    Vote vote = new Vote();
    vote.setReference(currentlySpectating.get());
    vote.setVoteType(voteType);
    vote.setVoteTime(clock.getTime());
    vote.setTwitchUser(twitchIrcNick);
    pm.makePersistent(vote);
    return true;
  }

  @Override
  public synchronized QueuedPlayer getNextPlayer(PersistenceManager pm) {
    PlayerQueue loadQueue = PlayerQueue.loadQueue(pm);
    return loadQueue.spectatingNext().orElse(null);
  }

  Optional<QueuedPlayer> pickAutoPlayer(PersistenceManager pm, QueuedPlayer currentPlayer) {
    List<ApiUser> recentlyActive = getRecentlyActive(pm);
    Map<Integer, Long> lastPlayTime = getLastPlayTimes(pm);
    Set<Integer> spectatingNotAllowed = getSpectateNotAllowed(pm);
    long time = clock.getTime();
    ToDoubleFunction<ApiUser> sortingProperty =
        user -> Math.pow(user.getRank() + 50, 2)
            / (double) Math.max(1, time - lastPlayTime.computeIfAbsent(user.getUserId(), x -> 0L));
    double min = Double.POSITIVE_INFINITY;
    ApiUser minArg = null;
    for (ApiUser apiUser : recentlyActive) {
      if (currentPlayer != null && currentPlayer.getPlayer().getUserId() == apiUser.getUserId()) {
        continue;
      }
      if (spectatingNotAllowed.contains(apiUser.getUserId())) {
        continue;
      }
      double val = sortingProperty.applyAsDouble(apiUser);
      if (val < min) {
        min = val;
        minArg = apiUser;
      }
    }
    if (minArg == null) {
      return Optional.empty();
    }
    try (JDOQuery<OsuUser> query =
        new JDOQuery<OsuUser>(pm).select(osuUser).from(osuUser)
            .where(osuUser.userId.eq(minArg.getUserId()))) {
      return Optional.of(pm.makePersistent(new QueuedPlayer(query.fetchOne(), QueueSource.AUTO,
          clock.getTime())));
    }
  }

  Set<Integer> getSpectateNotAllowed(PersistenceManager pm) {
    try (JDOQuery<Integer> query =
        new JDOQuery<>(pm).select(osuUser.userId).from(osuUser)
            .where(osuUser.allowsSpectating.not())) {
      return new HashSet<>(query.fetch());
    }
  }

  Map<Integer, Long> getLastPlayTimes(PersistenceManager pm) {
    Map<Integer, Long> lastPlayTime = new HashMap<>();
    try (JDOQuery<Tuple> queuedUsers =
        new JDOQuery<>(pm).select(queuedPlayer.player, queuedPlayer.startedAt.max())
            .from(queuedPlayer).where(queuedPlayer.state.eq(QueuedPlayer.DONE))
            .groupBy(queuedPlayer.player)) {
      queuedUsers.fetch().forEach(
          tuple -> lastPlayTime.put(tuple.get(0, OsuUser.class).getUserId(),
              tuple.get(1, long.class)));
    }
    return lastPlayTime;
  }

  List<ApiUser> getRecentlyActive(PersistenceManager pm) {
    List<ApiUser> recentlyActive;
    try (JDOQuery<ApiUser> query =
        new JDOQuery<ApiUser>(pm)
            .select(playerActivity.user)
            .from(playerActivity)
            .where(
                playerActivity.user.rank.loe(settings.getAutoSpecMaxRank()),
                playerActivity.lastActivity.goe(clock.getTime()
                    - settings.getAutoSpecMaxLastActivity()))) {
      recentlyActive = new ArrayList<>(query.fetch());
    }
    return recentlyActive;
  }

  @Override
  public synchronized void removeFromQueue(PersistenceManager pm, OsuUser player) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    Optional<QueuedPlayer> current = queue.currentlySpectating();
    if (current.isPresent()) {
      if (current.get().getPlayer().equals(player)) {
        advance(pm, queue);
        return;
      }
    }
    queue.queue.stream().filter(x -> x.getPlayer().equals(player))
        .forEach(x -> x.setState(QueuedPlayer.DONE));
  }
}
