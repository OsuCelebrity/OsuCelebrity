package me.reddev.osucelebrity.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.QueueUser;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.twitch.Twitch;

import org.tillerino.osuApiModel.OsuApiUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * Controller for what's shown on the screen. Currently all methods except run() are synchronized.
 * This has the potential to block other threads.
 * 
 * @author Tillerino
 */
@Slf4j
@RequiredArgsConstructor
public class Spectator implements Runnable {
  class QueuedPlayer implements Comparable<QueuedPlayer> {
    final long queuedAt = clock.getTime();

    final QueueUser user;

    public QueuedPlayer(QueueUser user) {
      super();
      this.user = user;
    }

    @Override
    public int compareTo(QueuedPlayer obj) {
      return Long.compare(queuedAt, obj.queuedAt);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof QueuedPlayer) {
        return Objects.equals(user, ((QueuedPlayer) obj).user);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return user.hashCode();
    }
  }

  final Twitch twitch;

  final Clock clock;

  final Osu osu;

  final CoreSettings settings;

  final ArrayList<QueuedPlayer> queue = new ArrayList<>();

  long spectatingSince = 0;

  long spectatingUntil = 0;

  boolean run = true;

  Thread runThread = null;

  QueueUser currentlySpectating = null;

  QueueUser spectatingNext = null;

  @Override
  public void run() {
    try {
      for (; run;) {
        long sleepUntil = loop();
        try {
          runThread = Thread.currentThread();
          clock.sleepUntil(sleepUntil);
        } catch (InterruptedException e) {
          // time to wake up and work!
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

  synchronized long loop() {
    if (currentlySpectating == null || spectatingUntil < clock.getTime()) {
      advance();
    } else if (spectatingUntil < clock.getTime() + 10000 && spectatingNext == null) {
      lockNext();
    }
    return Math.min(clock.getTime() + 100, spectatingUntil > clock.getTime() ? spectatingUntil
        : clock.getTime() + 100);
  }

  synchronized void lockNext() {
    spectatingNext = poll();
    if (spectatingNext != null) {
      // TODO send 10 second warning
    }
  }

  /**
   * Adds a player to the queue.
   * 
   * @return true if the player was added, false if they are already in the queue or currently being
   *         spectated.
   */
  public synchronized boolean enqueue(QueueUser user) {
    if (Objects.equals(currentlySpectating, user)) {
      return false;
    }
    if (Objects.equals(spectatingNext, user)) {
      return false;
    }
    if (queue.contains(new QueuedPlayer(user))) {
      return false;
    }
    queue.add(new QueuedPlayer(user));
    log.info("Queued " + user.getQueuedPlayer().getUserName());
    // wake spectator in the case that the queue was empty.
    wake();
    return true;
  }

  /**
   * Advances to the next player in queue.
   * 
   * @return True if successful, false if there is no next queued player.
   */
  public synchronized boolean advance() {
    QueueUser next = spectatingNext;
    if (next == null) {
      next = poll();
    } else {
      spectatingNext = null;
    }
    if (next == null) {
      return false;
    }
    // TODO say good-bye to the player currently being spectated
    startSpectating(next);
    return true;
  }

  synchronized QueueUser poll() {
    if (queue.isEmpty()) {
      return null;
    }
    Collections.sort(queue);
    return queue.remove(0).user;
  }

  private synchronized void startSpectating(QueueUser next) {
    currentlySpectating = next;
    spectatingSince = clock.getTime();
    spectatingUntil = spectatingSince + settings.getDefaultSpecDuration();
    OsuApiUser user = next.getQueuedPlayer();
    osu.notifyStarting(user);
    osu.startSpectate(user);
    QueueUser peek = peek();
    if (peek != null) {
      osu.notifySoon(peek.getQueuedPlayer());
    }
  }

  private synchronized QueueUser peek() {
    if (queue.isEmpty()) {
      return null;
    }
    Collections.sort(queue);
    return queue.get(0).user;
  }
}
