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
  public static final String QUEUE_DENIED = "%s has opted out of being spectated.";
  public static final String QUEUE_VOTED =
      "Voted for %s.";
  public static final String QUEUE_NOT_VOTED = "You already voted for %s.";
  public static final String QUEUE_SPECTATING = "%s is currently being spectated.";
  public static final String QUEUE_NEXT = "%s is next.";
  
  public static final String OFFLINE = "%s is offline.";
  
  /*
   * EXCEPTIONS
   */
  /**
   * I tried to be all weeb, but I'm not even convincing myself. Can someone make this weeb harder?
   * -Tillerino
   */
  public static final String EXCEPTION_INTERNAL = "Baka! This isn't working at all.";
  public static final String EXCEPTION_IO = "Error while accessing the osu! api. "
      + "Please try again later.";
  public static final String EXCEPTION_TIMEOUT = "Timeout while accessing the osu! api. "
      + "Please try again later!";
}
