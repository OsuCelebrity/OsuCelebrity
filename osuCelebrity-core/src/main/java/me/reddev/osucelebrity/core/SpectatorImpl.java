package me.reddev.osucelebrity.core;

import static me.reddev.osucelebrity.core.QQueueVote.queueVote;
import static me.reddev.osucelebrity.core.QQueuedPlayer.queuedPlayer;
import static me.reddev.osucelebrity.core.QVote.vote;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;
import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;
import static me.reddev.osucelebrity.twitch.QTwitchUser.twitchUser;
import static me.reddev.osucelebrity.util.ExecutorServiceHelper.detachAndSchedule;

import com.google.common.base.Objects;

import com.querydsl.core.Tuple;
import com.querydsl.jdo.JDOQuery;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.JdoQueryUtil;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.api.CurrentPlayerService;
import me.reddev.osucelebrity.core.api.DisplayQueuePlayer;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osu.PlayerActivity;
import me.reddev.osucelebrity.osu.PlayerStatus;
import me.reddev.osucelebrity.osu.PlayerStatus.PlayerStatusType;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitch.SceneSwitcher;
import me.reddev.osucelebrity.twitch.Twitch;
import me.reddev.osucelebrity.twitch.TwitchUser;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

/**
 * Controller for what's shown on the screen. Currently all methods except run() are synchronized.
 * This has the potential to block other threads.
 * 
 * @author Tillerino
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SpectatorImpl implements SpectatorImplMBean, Spectator {
  final Twitch twitch;

  final Clock clock;

  final Osu osu;

  final CoreSettings settings;

  final PersistenceManagerFactory pmf;

  final OsuApi osuApi;
  
  final ExecutorService exec;
  
  final StatusWindow statusWindow;
  
  final SceneSwitcher sceneSwitcher;
  
  final TwitchApi twitchApi;
  
  /**
   * Perform one loop iteration.
   */
  public void loop() {
    try {
      PersistenceManager pm = pmf.getPersistenceManager();
      try {
        loop(pm);
      } finally {
        pm.close();
      }
    } catch (Exception e) {
      log.error("Exception in spectator", e);
    }
  }

  synchronized void loop(PersistenceManager pm) {
    Transaction transaction = pm.currentTransaction();
    transaction.begin();
    try {
      PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
      statusWindow.setQueue(queue.queue);
      
      statusWindow.setFrozen(frozen);
      if (frozen) {
        updateRemainingTime(pm, queue);
        transaction.commit();
        return;
      }
      
      Optional<QueuedPlayer> current = queue.currentlySpectating();
      Optional<QueuedPlayer> next = lockNext(pm, queue);
      long time = clock.getTime();
  
      if (current.isPresent()) {
        SkipReason shouldSkip = status.shouldSkip(pm, current.get());
        if (shouldSkip != null) {
          QueuedPlayer newPlayer = advance(pm, queue);
          if (newPlayer != null) {
            detachAndSchedule(exec, log, pm, twitch::announceAdvance, shouldSkip, current
                .get().getPlayer(), newPlayer.getPlayer());
          }
        } else {
          if (queue.spectatingUntil() <= time) {
            if (next.isPresent()) {
              QueuedPlayer newPlayer = advance(pm, queue);
              if (newPlayer != null) {
                detachAndSchedule(exec, log, pm, twitch::announceAdvance, null, current
                    .get().getPlayer(), newPlayer.getPlayer());
              }
            } else {
              if (queue.spectatingUntil() <= time
                  - (settings.getAutoSpecTime() - settings.getDefaultSpecDuration())) {
                QueuedPlayer newPlayer = advance(pm, queue);
                if (newPlayer != null) {
                  detachAndSchedule(exec, log, pm, twitch::announceAdvance, null, current
                      .get().getPlayer(), newPlayer.getPlayer());
                }
              }
            }
          }
        }
      } else {
        QueuedPlayer newPlayer = advance(pm, queue);
        if (newPlayer != null) {
          detachAndSchedule(exec, log, pm, twitch::announceAdvance, null, null,
              newPlayer.getPlayer());
        }
      }
      
      updateRemainingTime(pm, queue);
      
      transaction.commit();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }
  }

  class Status {
    long playerStarted = -1;
    long lastStatusChange = -1;
    OsuStatus lastStatus = null;
    PlayerStatus ingameStatus = null;
    long lastIngameStatusPoll = Long.MIN_VALUE;

    @SuppressFBWarnings(value = "NP",
        justification = "lastStatus = currentStatus raises a weird bug")
    SkipReason shouldSkip(PersistenceManager pm, QueuedPlayer player) {
      if (playerStarted < 0) {
        playerStarted = clock.getTime();
      }
      
      OsuStatus currentStatus = osu.getClientStatus();
      try {
        if (lastStatusChange == -1 || !Objects.equal(currentStatus, lastStatus)) {
          lastStatusChange = clock.getTime();
          handleScenes(currentStatus.getType());
          if (currentStatus.getType() == Type.PLAYING) {
            for (BannedFilter filter : pm.getExtent(BannedFilter.class)) {
              if (currentStatus.getDetail().startsWith(filter.getStartsWith())) {
                return SkipReason.BANNED_MAP;
              }
            }
          }
          return null;
        }
        if (currentStatus.getType() == Type.PLAYING) {
          return null;
        }
        if (lastIngameStatusPoll < clock.getTime() - 1000) {
          lastIngameStatusPoll = clock.getTime();
          detachAndSchedule(exec, log, pm, osu::pollIngameStatus, player.getPlayer());
        }
        if (currentStatus.getType() == Type.IDLE
            && lastStatusChange <= clock.getTime() - settings.getOfflineTimeout()) {
          return SkipReason.OFFLINE;
        }
        long idleTimeout = settings.getIdleTimeout();
        if (ingameStatus != null && (ingameStatus.getType() == PlayerStatusType.PLAYING
            || ingameStatus.getType() == PlayerStatusType.MULTIPLAYING)) {
          idleTimeout *= 2;
        }
        if (ingameStatus != null && ingameStatus.getType() == PlayerStatusType.AFK) {
          idleTimeout /= 2;
        }
        if (currentStatus != null && currentStatus.getType() == Type.WATCHING
            && lastStatusChange <= clock.getTime() - idleTimeout) {
          return SkipReason.IDLE;
        }
        return null;
      } finally {
        lastStatus = currentStatus;
      }
    }

    void handleScenes(Type type) {
      statusWindow.setStatus(type);
      try {
        if (type == Type.PLAYING) {
          sceneSwitcher.changeScene("Osu Client");
        } else if (playerStarted > clock.getTime() - 10000L) {
          sceneSwitcher.changeScene("Idle");
        }
      } catch (Exception e) {
        log.error("Exception while changing scenes", e);
      }
    }
  }

  Status status = new Status();

  private boolean frozen = false;
  
  public static double penalty(long defaultSpecDuration, long timePlayed) {
    double quot = Math.min(1, defaultSpecDuration / (double) timePlayed);
    return 0.4 * (1 - Math.pow(quot, 1));
  }

  private void updateRemainingTime(PersistenceManager pm, PlayerQueue queue) {
    Optional<QueuedPlayer> currentlySpectating = queue.currentlySpectating();
    if (!currentlySpectating.isPresent()) {
      return;
    }
    QueuedPlayer current = currentlySpectating.get();
    long time = clock.getTime();

    double approval = getApproval(pm, current);
    statusWindow.setRawApproval(approval);
    long timePlayed = time - current.getStartedAt();
    approval -= penalty(settings.getDefaultSpecDuration(), timePlayed);
    
    OsuStatus readStatus = status.lastStatus;
    boolean playing = readStatus != null && readStatus.getType() == Type.PLAYING;

    long drain = time - current.getLastRemainingTimeUpdate();
    
    int queueSize =
        (int) queue.queue.stream()
            .filter(queuedPlayer -> queuedPlayer.getQueueSource() != QueueSource.AUTO).count();
    queueSize = Math.max(1, queueSize);
    if (queueSize <= settings.getShortQueueLength()) {
      drain *= .5 * (1D + (queueSize - 1D) / (settings.getShortQueueLength() - 1D));
    }
    
    final long adjustedDrain;
    if (frozen || (!playing && timePlayed < settings.getDefaultSpecDuration())) {
      adjustedDrain = 0;
    } else if (approval > .75) {
      adjustedDrain = -drain;
    } else if (approval >= .5) {
      adjustedDrain = 0;
    } else if (approval < .25) {
      adjustedDrain = drain * 2;
    } else {
      adjustedDrain = drain;
    }
    long newStoppingAt =
        current.getStoppingAt() + time - current.getLastRemainingTimeUpdate() - adjustedDrain;
    newStoppingAt = Math.min(newStoppingAt, time + settings.getDefaultSpecDuration());
    
    current.setStoppingAt(newStoppingAt);
    // no votes are NaN => regular drain

    current.setLastRemainingTimeUpdate(time);
    
    // report numbers
    statusWindow.setApproval(approval);
    statusWindow.setRemainingTime(current.getStoppingAt() - clock.getTime());
  }

  double getApproval(PersistenceManager pm, QueuedPlayer queuedPlayer) {
    List<Vote> votes = getVotes(pm, queuedPlayer);
    long upVotes =
        votes.stream().filter(x -> x.getVoteType().equals(VoteType.UP)).count();
    return upVotes / (double) votes.size();
  }

  @Override
  public List<Vote> getVotes(PersistenceManager pm, QueuedPlayer queuedPlayer) {
    try (JDOQuery<Vote> query = new JDOQuery<>(pm)) {
      query
          .select(vote)
          .from(vote)
          .where(vote.reference.eq(queuedPlayer),
              vote.voteTime.goe(clock.getTime() - settings.getVoteWindow()))
          .orderBy(vote.voteTime.desc());
      Set<String> voteKey = new HashSet<>();
      return query.fetch().stream().filter(vote -> voteKey.add(vote.getTwitchUser()))
          .collect(Collectors.toList());
    }
  }

  synchronized Optional<QueuedPlayer> lockNext(PersistenceManager pm, PlayerQueue queue) {
    Optional<QueuedPlayer> spectatingNext = queue.spectatingNext();
    if (!spectatingNext.isPresent()) {
      spectatingNext = queue.poll(pm);
      if (spectatingNext.isPresent()) {
        setNext(pm, queue, spectatingNext.get());
      }
    }
    return spectatingNext;
  }

  private void setNext(PersistenceManager pm, PlayerQueue queue, QueuedPlayer next) {
    log.debug("Setting {} to state {}", next.getPlayer().getUserName(), QueuedPlayer.NEXT);
    next.setState(QueuedPlayer.NEXT);
    if (next.isNotify() && queue.currentlySpectating().isPresent()
        && queue.currentlySpectating().get().getStoppingAt() > clock.getTime()) {
      detachAndSchedule(exec, log, pm, osu::notifyNext, next.getPlayer());
    }
  }

  @Override
  public EnqueueResult enqueue(PersistenceManager pm, QueuedPlayer user, boolean selfqueue,
      String twitchUser, boolean online) throws IOException {
    /*
     * This method is NOT synchronized, we can do expensive calls.
     */
    if (settings.getMinPlayCount() > 0 || settings.getMaxLastActivity() > 0) {
      ApiUser userData =
          osuApi.getUserData(user.getPlayer().getUserId(), user.getPlayer().getGameMode(), pm, 0L);
      if (userData == null || userData.getPlayCount() < settings.getMinPlayCount()) {
        log.debug("{}'s playcount is too low", user.getPlayer().getUserName());
        return EnqueueResult.FAILURE;
      }
      PlayerActivity activity = osuApi.getPlayerActivity(userData, pm, 1L);
      if (settings.getMaxLastActivity() > 0 && !selfqueue
          && activity.getLastActivity() < clock.getTime() - settings.getMaxLastActivity()) {
        log.debug("{} has not been active", user.getPlayer().getUserName());
        return EnqueueResult.FAILURE;
      }
    }
    return doEnqueue(pm, user, selfqueue, twitchUser, online);
  }

  /**
   * enqueue without activity/rank checks.
   * @param twitchUser the enqueueing twitch user's name or null if queued from somewhere else
   * @param online if the user is online. if false, a vote is attempted first.
   */
  EnqueueResult doEnqueue(PersistenceManager pm, QueuedPlayer user, boolean selfqueue,
      String twitchUser, boolean online) {
    if (!selfqueue && !user.getPlayer().isAllowsSpectating()) {
      return EnqueueResult.DENIED;
    }
    if (user.getPlayer().getTimeOutUntil() > clock.getTime()) {
      return EnqueueResult.DENIED;
    }
    synchronized (this) {
      PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
      Optional<QueuedPlayer> current = queue.currentlySpectating();
      if (queue.contains(user)) {
        user = queue.find(user).get();
        if (current.isPresent() && current.get().equals(user)) {
          return EnqueueResult.CURRENT;
        }
        Optional<QueuedPlayer> next = queue.spectatingNext();
        if (next.isPresent() && next.get().equals(user)) {
          return EnqueueResult.NEXT;
        }
        if (twitchUser == null) {
          return EnqueueResult.FAILURE;
        }
        if (voteQueue(pm, user, twitchUser)) {
          return EnqueueResult.VOTED;
        } else {
          return EnqueueResult.NOT_VOTED;
        }
      }
      if (!online) {
        return EnqueueResult.CHECK_ONLINE;
      }
      queue.add(pm, user);
      if (twitchUser != null) {
        voteQueue(pm, user, twitchUser);
      }
      log.info("Queued " + user.getPlayer().getUserName());
      if (user.isNotify() && !user.equals(lockNext(pm, queue).orElse(null))) {
        detachAndSchedule(exec, log, pm, osu::notifyQueued, user.getPlayer(),
            queue.playerAt(pm, user));
      }
      return EnqueueResult.SUCCESS;
    }
  }

  @Override
  public synchronized boolean advanceConditional(PersistenceManager pm, String expectedUser) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    if (!queue.isCurrent(expectedUser)) {
      return false;
    }
    OsuUser oldPlayer = queue.currentlySpectating().get().getPlayer();
    QueuedPlayer newPlayer = advance(pm, queue);
    if (newPlayer != null) {
      detachAndSchedule(exec, log, pm, twitch::announceAdvance, null, oldPlayer,
          newPlayer.getPlayer());
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean extendConditional(PersistenceManager pm, String expectedUser) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    if (!queue.isCurrent(expectedUser)) {
      return false;
    }
    QueuedPlayer current = queue.currentlySpectating().get();
    current.setStoppingAt(Math.min(clock.getTime(), current.getStoppingAt())
        + settings.getDefaultSpecDuration());
    return true;
  }

  @Override
  public synchronized boolean promote(PersistenceManager pm, OsuUser ircUser) {
    QueuedPlayer queueRequest = new QueuedPlayer(ircUser, QueueSource.TWITCH, clock.getTime());

    EnqueueResult enqueueResult = doEnqueue(pm, queueRequest, false, null, true);
    if (enqueueResult == EnqueueResult.DENIED) {
      // Don't force spectate a denied player
      return false;
    }
    if (enqueueResult == EnqueueResult.CURRENT) {
      detachAndSchedule(exec, log, pm, osu::startSpectate, ircUser);
      return true;
    }

    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    QueuedPlayer original = queueRequest;
    queueRequest = queue.stream().filter(x -> x.equals(original)).findFirst().get();

    Optional<QueuedPlayer> nextUser = queue.spectatingNext();
    // Revert the next player
    if (nextUser.isPresent()) {
      log.debug("Reverting {} to state {}", nextUser.get().getPlayer().getUserName(),
          QueuedPlayer.QUEUED);
      nextUser.get().setState(QueuedPlayer.QUEUED);
    }
    
    final Optional<QueuedPlayer> current = queue.currentlySpectating();

    log.debug("Setting {} to state {}", queueRequest.getPlayer().getUserName(), QueuedPlayer.NEXT);
    queueRequest.setState(QueuedPlayer.NEXT);
    queue = PlayerQueue.loadQueue(pm, clock);
    advance(pm, queue);

    if (current.isPresent()) {
      setNext(pm, queue, current.get());
    }
    return true;
  }

  @CheckForNull
  QueuedPlayer advance(PersistenceManager pm, PlayerQueue queue) {
    Optional<QueuedPlayer> current = queue.currentlySpectating();
    Optional<QueuedPlayer> next = queue.spectatingNext();
    if (!next.isPresent()) {
      next = queue.poll(pm);
    }
    if (!next.isPresent()) {
      next = pickAutoPlayer(pm, current.orElse(null));
      if (next.isPresent()) {
        queue.add(pm, next.get());
      }
    }
    if (!next.isPresent()) {
      return null;
    }
    startSpectating(pm, queue, next.get());
    return next.get();
  }
  
  private synchronized void startSpectating(PersistenceManager pm, 
      PlayerQueue queue, QueuedPlayer next) {
    Optional<QueuedPlayer> spectating = queue.currentlySpectating();
    if (spectating.isPresent()) {
      log.debug("Setting {} to state {}", spectating.get().getPlayer().getUserName(),
          QueuedPlayer.DONE);
      spectating.get().setState(QueuedPlayer.DONE);
      if (spectating.get().isNotify()) {
        detachAndSchedule(exec, log, pm, osu::notifyDone, spectating.get().getPlayer());
        sendEndStatistics(pm, spectating.get());
        detachAndSchedule(exec, log, pm, this::sendReplayLink, spectating.get());
      }
    }
    long time = clock.getTime();
    log.debug("Setting {} to state {}", next.getPlayer().getUserName(), QueuedPlayer.SPECTATING);
    next.setState(QueuedPlayer.SPECTATING);
    next.setStartedAt(time);
    next.setLastRemainingTimeUpdate(time);
    next.setStoppingAt(next.getStartedAt() + settings.getDefaultSpecDuration());
    OsuUser user = next.getPlayer();
    if (next.isNotify()) {
      detachAndSchedule(exec, log, pm, osu::notifyStarting, user);
    }
    detachAndSchedule(exec, log, pm, osu::startSpectate, user);
    status = new Status();
    statusWindow.newPlayer();
  }

  private void sendReplayLink(QueuedPlayer queuedPlayer) {
    try {
      URL replayLink = twitchApi.getReplayLink(queuedPlayer);
      if (replayLink == null) {
        return;
      }
      osu.message(queuedPlayer.getPlayer(),
          String.format(OsuResponses.REPLAY, replayLink));
    } catch (Exception e) {
      UserException.handleException(log, e, null);
    }
  }

  @Override
  public synchronized QueuedPlayer getCurrentPlayer(PersistenceManager pm) {
    return PlayerQueue.loadQueue(pm, clock).currentlySpectating().orElse(null);
  }

  @Override
  public synchronized boolean vote(PersistenceManager pm, String twitchIrcNick, VoteType voteType,
      String command) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    Optional<QueuedPlayer> currentlySpectating = queue.currentlySpectating();
    if (!currentlySpectating.isPresent()) {
      return false;
    }
    if (currentlySpectating.get().getStartedAt() > clock.getTime() - settings.getStreamDelay()) {
      return false;
    }
    Vote vote = new Vote();
    vote.setReference(currentlySpectating.get());
    vote.setVoteType(voteType);
    vote.setVoteTime(clock.getTime());
    vote.setTwitchUser(twitchIrcNick);
    vote.setCommand(command);
    pm.makePersistent(vote);
    return true;
  }
  
  boolean voteQueue(PersistenceManager pm, QueuedPlayer player, @Nonnull String voter) {
    if (hasVoted(pm, player, voter)) {
      return false;
    }
    if (voter.startsWith(QueueVote.TWITCH)) {
      Optional<TwitchUser> twitchUserObj =
          JdoQueryUtil.getUnique(pm, twitchUser,
              twitchUser.user.name.eq(voter.substring(QueueVote.TWITCH.length())));
      if (twitchUserObj.isPresent()) {
        OsuUser osuUser = twitchUserObj.get().getOsuUser();
        if (osuUser != null) {
          if (hasVoted(pm, player, QueueVote.OSU + osuUser.getUserId())) {
            return false;
          }
        }
      }
    }
    if (voter.startsWith(QueueVote.OSU)) {
      Optional<TwitchUser> twitchUserObj =
          JdoQueryUtil.getUnique(pm, twitchUser, twitchUser.osuUser.userId.eq(Integer.valueOf(voter
              .substring(QueueVote.OSU.length()))));
      if (twitchUserObj.isPresent()) {
        if (hasVoted(pm, player, QueueVote.TWITCH + twitchUserObj.get().getUser().getName())) {
          return false;
        }
      }
    }
    pm.makePersistent(new QueueVote(player, voter));
    return true;
  }

  private boolean hasVoted(PersistenceManager pm, QueuedPlayer player, String voter) {
    return JdoQueryUtil.getUnique(pm, queueVote, queueVote.reference.eq(player),
        queueVote.twitchUser.eq(voter)).isPresent();
  }

  @Override
  public synchronized QueuedPlayer getNextPlayer(PersistenceManager pm) {
    PlayerQueue loadQueue = PlayerQueue.loadQueue(pm, clock);
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
      return Optional.of(new QueuedPlayer(query.fetchOne(), QueueSource.AUTO, clock.getTime()));
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
  public synchronized QueuedPlayer removeFromQueue(PersistenceManager pm, OsuUser player) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    return doRemoveFromQueue(pm, player, queue);
  }

  @CheckForNull
  private QueuedPlayer doRemoveFromQueue(PersistenceManager pm, OsuUser player, PlayerQueue queue) {
    Optional<QueuedPlayer> current = queue.currentlySpectating();
    QueuedPlayer newPlayer = null;
    if (current.isPresent()) {
      if (current.get().getPlayer().equals(player)) {
        newPlayer = advance(pm, queue);
      }
    }
    queue.stream().filter(x -> x.getPlayer().equals(player))
        .forEach(x -> x.setState(QueuedPlayer.CANCELLED));
    return newPlayer;
  }

  @Override
  public int getQueueSize(PersistenceManager pm) {
    return PlayerQueue.loadQueue(pm, clock).getSize();
  }
  
  @Override
  public int getQueuePosition(PersistenceManager pm, OsuUser player) {
    int pos = PlayerQueue.loadQueue(pm, clock)
        .playerAt(pm, new QueuedPlayer(player, QueueSource.AUTO, clock.getTime())); 
    return pos == -1 ? -1 : pos;
  }
  
  @Override
  public List<DisplayQueuePlayer> getCurrentQueue(PersistenceManager pm) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    queue.ensureSorted(pm);
    Map<QueuedPlayer, Double> votes = queue.getVotes(pm);
    List<DisplayQueuePlayer> result = new ArrayList<>();
    for (QueuedPlayer player : queue.queue) {
      if (player.getState() < QueuedPlayer.QUEUED) {
        continue;
      }
      long timeInQueue = clock.getTime() - player.getQueuedAt();
      String timeString = CurrentPlayerService.formatDuration(timeInQueue);
      String votesString =
          player.getBoost() > 0 ? "∞" : String.valueOf(votes.getOrDefault(player, 0D).intValue());
      result.add(new DisplayQueuePlayer(player.getPlayer().getUserId(),
          player.getPlayer().getUserName(), timeString, votesString));
    }
    return result;
  }
  
  @Override
  public void sendEndStatistics(PersistenceManager pm, QueuedPlayer player) {
    try (JDOQuery<Vote> query =
        new JDOQuery<>(pm).select(vote).from(vote)
            .where(vote.reference.eq(player))) {
      List<Vote> votes = query.fetchResults().getResults();
      
      long danks = votes.stream().filter(x -> x.getVoteType() == VoteType.UP).count();
      long skips = votes.stream().filter(x -> x.getVoteType() == VoteType.DOWN).count();
      
      double dankScore = 0;

      if (danks + skips != 0) {
        dankScore = (double) danks / (danks + skips);
      }
      
      //Make sure it's all happy trees
      if (dankScore >= 0.5) {
        detachAndSchedule(exec, log, pm, osu::notifyStatistics, player.getPlayer(), danks, skips);
      }
    }
  }
  
  @Override
  public synchronized void reportStatus(PersistenceManager pm, PlayerStatus status) {
    PlayerQueue queue = null;
    if (status.getType() == PlayerStatusType.OFFLINE) {
      // Just remove them from the queue. We can refine this behaviour later.
      queue = PlayerQueue.loadQueue(pm, clock);
      log.debug("{} is offline. removing from queue", status.getUser().getUserName());
      QueuedPlayer newPlayer = removeFromQueue(pm, status.getUser());
      if (newPlayer != null) {
        detachAndSchedule(exec, log, pm, twitch::announceAdvance, SkipReason.OFFLINE,
            status.getUser(), newPlayer.getPlayer());
      }
    }
    if (queue == null) {
      queue = PlayerQueue.loadQueue(pm, clock);
    }
    Optional<QueuedPlayer> current = queue.currentlySpectating();
    if (current.isPresent() && current.get().getPlayer().equals(status.getUser())) {
      if (this.status.ingameStatus != null
          && this.status.ingameStatus.getReceivedAt() > status.getReceivedAt()) {
        return;
      }
      this.status.ingameStatus = status;
    }
  }

  @Override
  public void performEnqueue(PersistenceManager persistenceManager, QueuedPlayer queueRequest,
      String requestingUser, Logger log, Consumer<String> reply, Consumer<String> replyNegative)
      throws IOException {
    /*
     * This method is NOT synchronized, we can do expensive calls.
     */
    EnqueueResult result = enqueue(persistenceManager, queueRequest, false, requestingUser, false);
    OsuUser requestedUser = queueRequest.getPlayer();
    if (result == EnqueueResult.CHECK_ONLINE) {
      queueRequest.setPlayer(persistenceManager.detachCopy(queueRequest.getPlayer()));
      osu.pollIngameStatus(requestedUser, (pm, status) -> {
          if (status.getType() == PlayerStatusType.OFFLINE) {
            replyNegative.accept(String.format(OsuResponses.OFFLINE, requestedUser.getUserName()));
          } else {
            queueRequest.setPlayer(pm.makePersistent(queueRequest.getPlayer()));
            EnqueueResult retryResult = enqueue(pm, queueRequest, false, requestingUser, true);

            replyEnqueue(retryResult, requestedUser.getUserName(), reply, replyNegative);
          }
        });
    } else {
      replyEnqueue(result, requestedUser.getUserName(), reply, replyNegative);
    }
  }
  
  void replyEnqueue(EnqueueResult result, String requestedUser, Consumer<String> reply,
      Consumer<String> replyNegative) {
    if (result == EnqueueResult.SUCCESS || result == EnqueueResult.VOTED) {
      reply.accept(result.formatResponse(requestedUser));
    } else {
      replyNegative.accept(result.formatResponse(requestedUser));
    }
  }
  
  @Override
  public synchronized boolean boost(PersistenceManager pm, OsuUser ircUser) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    
    Optional<QueuedPlayer> inQueue =
        queue.stream().filter(x -> x.getPlayer().equals(ircUser)).findFirst();

    if (!inQueue.isPresent()) {
      return false;
    }
    
    inQueue.get().setBoost(1);
    return true;
  }
  
  @Override
  public synchronized void addBannedMapFilter(PersistenceManager pm, String startsWith) {
    BannedFilter filter = new BannedFilter(startsWith);

    pm.makePersistent(filter);


    PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
    Optional<QueuedPlayer> current = queue.currentlySpectating();
    if (current.isPresent() && status.lastStatus != null
        && status.lastStatus.getType() == Type.PLAYING) {
      String detail = status.lastStatus.getDetail();
      if (detail == null) {
        throw new RuntimeException("if playing a status should be present");
      }
      if (!detail.startsWith(startsWith)) {
        return;
      }
      QueuedPlayer newPlayer = advance(pm, queue);
      if (newPlayer != null) {
        detachAndSchedule(exec, log, pm, twitch::announceAdvance, SkipReason.BANNED_MAP, current
            .get().getPlayer(), newPlayer.getPlayer());
      }
    }
  }

  @Override
  public synchronized void purgeQueue() {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      PlayerQueue queue = PlayerQueue.loadQueue(pm, clock);
      queue.stream().filter(player -> player.getState() > QueuedPlayer.NEXT)
          .forEach(player -> player.setState(QueuedPlayer.CANCELLED));
    } finally {
      pm.close();
    }
  }
  
  @Override
  public synchronized void setFrozen(boolean freeze) {
    frozen = freeze;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }
}
