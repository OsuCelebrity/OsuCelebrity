package me.reddev.osucelebrity.core;

import static me.reddev.osucelebrity.core.QQueuedPlayer.queuedPlayer;

import com.querydsl.jdo.JDOQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.jdo.PersistenceManager;


public class PlayerQueue {
  static final Comparator<QueuedPlayer> comparator = Comparator.comparing(QueuedPlayer::getState)
      .thenComparing(QueuedPlayer::getQueuedAt);
  List<QueuedPlayer> queue;

  PlayerQueue(List<QueuedPlayer> queue) {
    super();
    this.queue = new ArrayList<>(queue);
    Collections.sort(this.queue, comparator);
  }

  Optional<QueuedPlayer> currentlySpectating() {
    return queue.stream()
        .filter(player -> player.getState() == QueuedPlayer.SPECTATING)
        .findFirst();
  }

  long spectatingUntil() {
    return currentlySpectating()
        .orElseThrow(() -> new IllegalStateException("not spectating anybody"))
        .getStoppingAt();
  }
  
  int playerAt(QueuedPlayer player) {
    return queue.indexOf(player);
  }

  Optional<QueuedPlayer> spectatingNext() {
    return queue.stream()
        .filter(player -> player.getState() == QueuedPlayer.NEXT)
        .findFirst();
  }

  Optional<QueuedPlayer> poll() {
    return queue.stream()
        .filter(player -> player.getState() > QueuedPlayer.SPECTATING)
        .findFirst();
  }

  static PlayerQueue loadQueue(PersistenceManager pm) {
    JDOQuery<Object> query = new JDOQuery<>(pm);
    try {
      PlayerQueue queue = new PlayerQueue(query
          .select(queuedPlayer)
          .from(queuedPlayer)
          .where(queuedPlayer.state.goe(QueuedPlayer.SPECTATING))
          .fetch());
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
    return (int) queue.stream().filter(x -> x.getState() != QueuedPlayer.SPECTATING).count();
  }
}
