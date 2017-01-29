package me.reddev.osucelebrity.core;

import static me.reddev.osucelebrity.osuapi.QApiUser.apiUser;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.Osu.PollStatusConsumer;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osu.PlayerStatus;
import me.reddev.osucelebrity.osu.PlayerStatus.PlayerStatusType;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.tillerino.osuApiModel.GameModes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AutoQueue {
  final Osu osu;
  final OsuApi osuApi;
  final Spectator spectator;
  final Clock clock;
  final PersistenceManagerFactory pmf;
  final CoreSettings settings;

  static ToDoubleFunction<ApiUser> probability = user -> Math.pow(1 - 5E-3, user.getRank() + 50)
      * (user.getGameMode() == GameModes.OSU ? 1 : 1 / 9d);

  Semaphore semaphore = new Semaphore(1);

  /**
   * Perform one attempt to auto-queue. Logs exceptions.
   */
  public void loop() {
    PersistenceManager pm = pmf.getPersistenceManager();

    try {
      loop(pm);
    } catch (Exception e) {
      log.error("exception", e);
    } finally {
      pm.close();
    }
  }

  void loop(PersistenceManager pm) throws InterruptedException {
    if (spectator.getQueueSize(pm) >= settings.getAutoQueueMaxSize()) {
      return;
    }
    
    if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
      log.warn("last auto q poll took too long");
      semaphore = new Semaphore(1);
      semaphore.acquire();
    }
    int userId = drawUserId(pm);
    if (userId < 0) {
      semaphore.release();
      return;
    }
    OsuUser user;
    try {
      user = osuApi.getUser(userId, pm, TimeUnit.DAYS.toMillis(1));
    } catch (IOException e) {
      semaphore.release();
      log.warn("Osu api while refreshing user in auto queue", e);
      return;
    }
    if (user == null) {
      semaphore.release();
      return;
    }
    pollStatus(user);
  }

  final LinkedList<Integer> lastDraws = new LinkedList<>();
  final Set<Integer> lastDrawsAsSet = new HashSet<>();

  int drawUserId(PersistenceManager pm) {
    double sum = 0d;
    TreeMap<Double, ApiUser> distribution = new TreeMap<>();
    List<ApiUser> users = getTopPlayers(pm);
    if (users.isEmpty()) {
      log.warn("no top players");
      return -1;
    }
    for (ApiUser user : users) {
      if (lastDrawsAsSet.contains(user.getUserId())) {
        continue;
      }
      double prob = probability.applyAsDouble(user);
      distribution.put(sum, user);
      sum += prob;
    }
    if (sum == 0d) {
      lastDrawsAsSet.remove(lastDraws.removeFirst());
      return -1;
    }
    double rnd = Math.random() * sum;
    Entry<Double, ApiUser> floorEntry = distribution.floorEntry(rnd);
    if (floorEntry == null) {
      throw new RuntimeException(rnd + " in " + sum);
    }
    int userId = floorEntry.getValue().getUserId();
    if (lastDraws.size() >= 100) {
      lastDrawsAsSet.remove(lastDraws.removeFirst());
    }
    lastDraws.add(userId);
    lastDrawsAsSet.add(userId);
    return userId;
  }

  List<ApiUser> getTopPlayers(PersistenceManager pm) {
    try (JDOQuery<ApiUser> query = new JDOQuery<>(pm).select(apiUser).from(apiUser)) {
      return new ArrayList<>(query.where(apiUser.rank.loe(1000), apiUser.rank.goe(1)).fetch());
    }
  }

  private void pollStatus(OsuUser user) {
    Semaphore currentSemaphore = semaphore;
    PollStatusConsumer action = new PollStatusConsumer() {
      @Override
      public void accept(PersistenceManager pm, PlayerStatus status) throws IOException {
        currentSemaphore.release();
        if (status.getType() == PlayerStatusType.PLAYING) {
          QueuedPlayer queueRequest =
              new QueuedPlayer(status.getUser(), QueueSource.AUTO, clock.getTime());
          EnqueueResult result = spectator.enqueue(pm, queueRequest, false, null, true);
          log.debug("auto-queue: {}", result.formatResponse(status.getUser().getUserName()));
        }
      }
    };
    osu.pollIngameStatus(user, action);
  }
}
