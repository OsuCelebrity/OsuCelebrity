package me.reddev.osucelebrity;

/**
 * Using static final fields instead of interface methods makes sense here because it enables us to
 * do some static code analysis for the {@link String#format(String, Object...)} method.
 */
public class TwitchResponses extends Responses {
  public static final String INVALID_FORMAT_QUEUE = "Expected !queue {username}";
}
