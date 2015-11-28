package me.reddev.osucelebrity;

/**
 * Using static final fields instead of interface methods makes sense here because it enables us to
 * do some static code analysis for the {@link String#format(String, Object...)} method.
 */
public class TwitchResponses extends Responses {
  public static final String INVALID_FORMAT_QUEUE = "Expected !queue {username}";

  public static final String SKIPPED_OFFLINE = "It appears %s is offline.";

  public static final String SKIPPED_IDLE = "%s isn't doing anything. Let's move on.";

  public static final String SKIPPED_FORCE = "%s was skipped by %s.";

  public static final String SPECTATE_FORCE = "%s has force spectated %s";

  public static final String TIMEOUT = "%s has been timed out for %d minutes";
}
