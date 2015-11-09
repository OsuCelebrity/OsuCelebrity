package me.reddev.osucelebrity.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.twitch.Twitch;

import java.util.Optional;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * Controller for what's shown on the screen. Currently all methods except run() are synchronized.
 * This has the potential to block other threads.
 * 
 * @author Tillerino
 */
@Slf4j
@RequiredArgsConstructor
public class Spectator implements Runnable {
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
    } else if (queue.spectatingUntil() < clock.getTime() + 10000
        && !queue.spectatingNext().isPresent()) {
      lockNext(queue);
    }
    long until = queue.currentlySpectating().isPresent() ? queue.spectatingUntil() : 0;
    return Math.min(clock.getTime() + 100,
        until > clock.getTime() ? until
            : clock.getTime() + 100);
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
  public synchronized boolean enqueue(PersistenceManager pm, QueuedPlayer user) {
    PlayerQueue queue = PlayerQueue.loadQueue(pm);
    if (queue.contains(user)) {
      return false;
    }
    pm.makePersistent(user);
    log.info("Queued " + user.getPlayer().getUserName());
    // wake spectator in the case that the queue was empty.
    wake();
    return true;
  }

  /**
   * Advances to the next player in queue.
   * 
   * @return True if successful, false if there is no next queued player.
   */
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
    next.setState(QueuedPlayer.SPECTATING);
    next.setStartedAt(clock.getTime());
    next.setStoppingAt(next.getStartedAt() + settings.getDefaultSpecDuration());
    OsuUser user = next.getPlayer();
    osu.notifyStarting(user);
    osu.startSpectate(user);
    Optional<QueuedPlayer> peek = queue.poll();
    if (peek.isPresent()) {
      osu.notifySoon(peek.get().getPlayer());
    }
  }
}
