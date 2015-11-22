package me.reddev.osucelebrity;

/**
 * Using static final fields instead of interface methods makes sense here because it enables us to
 * do some static code analysis for the {@link String#format(String, Object...)} method.
 */
public class OsuResponses extends Responses {
  /*
   * Notifications / allow deny spectating
   */
  
  public static final String MUTED = "You will no longer receive notifications. "
      + "Send !unmute to turn notifications back on.";
  public static final String UNMUTED = "Notifications have been turned on.";
  public static final String OPTOUT = "You will no longer be spectated by us. "
      + "Send !optin to allow us to spectate you again.";
  public static final String OPTIN = "You're now allowing us to spectate you.";
  
  static final String commands = "If you don't want to be spectated, send !optout. "
      + "If you don't want to receive these notifications, send !mute.";
  
  /*
   * Command response notifications
   */
  
  public static final String POSITION = "%s is position #%d in the queue.";
  public static final String NOT_IN_QUEUE = "%s is not in the queue.";
  
  /*
   * Spectate-cycle notifications
   */
  
  /**
   * To the player who was queued.
   */
  public static final String QUEUED = "Someone has requested to spectate you "
      + "on [http://www.twitch.tv/osucelebrity OsuCelebrity]. "
      + "I will notify you when you're next. "
      + commands;
  
  /**
   * To the player who will be spectated next.
   */
  public static final String SPECTATING_NEXT = "You're next in line to be spectated "
      + "on [http://www.twitch.tv/osucelebrity OsuCelebrity]. "
      + commands;
  
  /**
   * To the player who is being spectated now.
   */
  public static final String SPECTATING_NOW = "[http://www.twitch.tv/osucelebrity OsuCelebrity] "
      + "is now spectating you. "
      + commands;
  
  /**
   * To the player who was being spectated.
   */
  public static final String DONE_SPECTATING = "[http://www.twitch.tv/osucelebrity OsuCelebrity] "
      + "has moved on to the next player. Thanks for playing! "
      + commands;
}
