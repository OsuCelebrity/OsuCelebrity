package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.Responses;

/**
 * Result of a queue request.
 */
public enum EnqueueResult {
  /**
   * User was added to the queue.
   */
  SUCCESS(Responses.QUEUE_SUCCESSFUL),
  /**
   * User was not added to the queue.
   */
  FAILURE(Responses.QUEUE_UNSUCCESSFUL),
  /**
   * User was already in the queue, but the demand was registered.
   */
  VOTED(Responses.QUEUE_VOTED),
  /**
   * User does not allow spectating.
   */
  DENIED(Responses.QUEUE_DENIED);
  
  private final String response;

  private EnqueueResult(String response) {
    this.response = response;
  }
  
  /**
   * Returns a string describing the result.
   * 
   * @param targetUser the requestee.
   */
  public String formatResponse(String targetUser) {
    return String.format(response, targetUser);
  }
}
