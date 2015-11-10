package me.reddev.osucelebrity;

/**
 * General response strings. Using static final fields instead of interface methods makes sense here
 * because it enables us to do some static code analysis for the
 * {@link String#format(String, Object...)} method.
 */
public class Responses {
  public static final String INVALID_USER = "Invalid User (%s)";
  public static final String CURRENT_QUEUE = "Current users in queue: %d";
  public static final String NEXT_IN_QUEUE = "Next user in queue: %s";
  public static final String QUEUE_EMPTY = "The queue is currently empty.";
  public static final String CURRENT_PLAYER = "Current Player: %s";

  public static final String SELF_QUEUE_SUCCESSFUL = "You've been added to the queue!";
  public static final String SELF_QUEUE_UNSUCCESSFU = "You could not be added to the queue!";

  public static final String QUEUE_SUCCESSFUL = "%s was added to the queue.";
  public static final String QUEUE_UNSUCCESSFUL = "%s could not be added to the queue.";
  public static final String QUEUE_VOTED =
      "%s was already in the queue, but we'll make sure to see them soon.";
}
