package me.reddev.osucelebrity.core;

/**
 * Result of a queue request.
 */
public enum EnqueueResult {
  /**
   * User was added to the queue.
   */
  SUCCESS,
  /**
   * User was not added to the queue.
   */
  FAILURE,
  /**
   * User was already in the queue, but the demand was registered.
   */
  VOTED,
  /**
   * User does not allow spectating.
   */
  DENIED
}
