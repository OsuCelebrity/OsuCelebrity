package me.reddev.osucelebrity;

/**
 * Using static final fields instead of interface methods makes sense here because it enables us to
 * do some static code analysis for the {@link String#format(String, Object...)} method.
 */
public class OsuResponses {
  public static final String INVALID_USER = "Invalid User (%s)";

  public static final String CURRENT_QUEUE = "Current users in queue: %d";
  public static final String NEXT_IN_QUEUE = "Next user in queue: %s";
  public static final String QUEUE_EMPTY = "The queue is currently empty.";
  public static final String CURRENT_PLAYER = "Current Player: %s";

  public static final String ADDED_TO_QUEUE = "You've been added to the queue!";
  public static final String STARTING_SESSION = "You're up. Show us your best moves!";
}
