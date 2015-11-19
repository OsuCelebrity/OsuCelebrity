package me.reddev.osucelebrity;

/**
 * Using static final fields instead of interface methods makes sense here because it enables us to
 * do some static code analysis for the {@link String#format(String, Object...)} method.
 */
public class OsuResponses extends Responses {
  public static final String STARTING_SESSION = "You're up. Show us your best moves!";
  public static final String MUTED = "You will no longer receive notifications. "
      + "Send !unmute to turn notifications back on.";
  public static final String UNMUTED = "Notifications have been turned on.";
  public static final String OPTOUT = "You will no longer be spectated by us. "
      + "Send !optin to allow us to spectate you again.";
  public static final String OPTIN = "You're now allowing us to spectate you.";
}
