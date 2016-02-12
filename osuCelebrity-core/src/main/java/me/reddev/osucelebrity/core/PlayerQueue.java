package me.reddev.osucelebrity.core;

import static me.reddev.osucelebrity.core.QQueueVote.queueVote;
import static me.reddev.osucelebrity.core.QQueuedPlayer.queuedPlayer;
import com.querydsl.core.Tuple;
import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.jdo.PersistenceManager;

@RequiredArgsConstructor
public class PlayerQueue {
  final List<QueuedPlayer> queue;
  
  final Clock clock;
  
  boolean sorted = false;

  void ensureSorted(PersistenceManager pm) {
    if (sorted) {
      return;
    }
    doSort(pm);
    sorted = true;
  }

  void doSort(PersistenceManager pm) {
    if (queue.isEmpty()) {
      return;
    }
    Map<QueuedPlayer, Double> votes = getVotes(pm);
    long firstQueued = queue.stream().mapToLong(QueuedPlayer::getQueuedAt).min().getAsLong();
    double maxVotes =
        Math.max(1, votes.values().stream().mapToDouble(Double::doubleValue).max().orElse(0D));
    
    long time = clock.getTime();
    double maxQueueTime = Math.max(time - firstQueued, 1);
    
    Function<QueuedPlayer, Double> queueTimePlusVotes =
        player -> votes.getOrDefault(player, .5) / maxVotes + (time - player.getQueuedAt())
        / maxQueueTime;
        
    Collections.sort(
        this.queue,
        Comparator.comparing(QueuedPlayer::getState)
            .thenComparing(Comparator.comparing(QueuedPlayer::getBoost).reversed())
            .thenComparing(Comparator.comparing(queueTimePlusVotes).reversed())
            .thenComparing(QueuedPlayer::getQueuedAt));
  }

  Map<QueuedPlayer, Double> getVotes(PersistenceManager pm) {
    Map<QueuedPlayer, Double> votes;
    try (JDOQuery<Tuple> query =
        new JDOQuery<>(pm).select(queueVote.reference, queueVote.count()).from(queueVote)
            .where(queueVote.reference.state.goe(QueuedPlayer.NEXT)).groupBy(queueVote.reference)) {
      votes = new HashMap<>();
      query.fetch().forEach(
          tuple -> votes.put(tuple.get(0, QueuedPlayer.class), tuple.get(1, Long.class).doubleValue()));
      
    }
    return votes;
  }

  Optional<QueuedPlayer> currentlySpectating() {
    return stream().filter(player -> player.getState() == QueuedPlayer.SPECTATING)
        .findFirst();
  }

  long spectatingUntil() {
    return currentlySpectating().orElseThrow(
        () -> new IllegalStateException("not spectating anybody")).getStoppingAt();
  }

  int playerAt(PersistenceManager pm, QueuedPlayer player) {
    ensureSorted(pm);
    return queue.indexOf(player);
  }

  Optional<QueuedPlayer> spectatingNext() {
    return stream().filter(player -> player.getState() == QueuedPlayer.NEXT).findFirst();
  }

  Optional<QueuedPlayer> poll(PersistenceManager pm) {
    ensureSorted(pm);
    return stream().filter(player -> player.getState() > QueuedPlayer.SPECTATING).findFirst();
  }

  static PlayerQueue loadQueue(PersistenceManager pm, Clock clock) {
    JDOQuery<Object> query = new JDOQuery<>(pm);
    try {
      List<QueuedPlayer> result =
          query.select(queuedPlayer).from(queuedPlayer)
              .where(queuedPlayer.state.goe(QueuedPlayer.SPECTATING)).fetch();
      PlayerQueue queue = new PlayerQueue(new ArrayList<>(result), clock);
      return queue;
    } finally {
      query.close();
    }
  }

  public boolean contains(QueuedPlayer user) {
    return queue.contains(user);
  }

  void add(PersistenceManager pm, QueuedPlayer user) {
    if (user.getState() != QueuedPlayer.QUEUED) {
      throw new IllegalArgumentException();
    }
    if (queue.contains(user)) {
      throw new IllegalStateException();
    }
    pm.makePersistent(user);
    queue.add(user);
  }

  public int getSize() {
    return (int) stream().filter(x -> x.getState() != QueuedPlayer.SPECTATING).count();
  }

  /**
   * returns a stream over the current queue.
   * @return these are probably not in order.
   */
  public Stream<QueuedPlayer> stream() {
    return queue.stream();
  }

  public Optional<QueuedPlayer> find(QueuedPlayer user) {
    return stream().filter(user::equals).findFirst();
  }

  boolean isCurrent(String expectedUser) {
    Optional<QueuedPlayer> currentUser = currentlySpectating();
    if (!currentUser.isPresent()) {
      return false;
    }
    String userName = currentUser.get().getPlayer().getUserName();
    int distance = StringUtils.getLevenshteinDistance(userName, expectedUser);
    if (distance >= userName.length() / 2) {
      return false;
    }
    return true;
  }
}
