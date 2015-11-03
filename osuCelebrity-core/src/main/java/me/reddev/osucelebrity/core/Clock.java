package me.reddev.osucelebrity.core;

/**
 * Interface for time interaction for easy testing of queuing mechanisms.
 * 
 * @author Tillerino
 */
public interface Clock {
  long getTime();

  void sleepUntil(long time) throws InterruptedException;
}
