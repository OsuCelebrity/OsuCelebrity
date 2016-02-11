package me.reddev.osucelebrity;

/**
 * Using static final fields instead of interface methods makes sense here because it enables us to
 * do some static code analysis for the {@link String#format(String, Object...)} method.
 */
public class TwitchResponses extends Responses {
  public static final String INVALID_FORMAT_QUEUE = "Expected !queue {username}";

  public static final String SKIPPED_OFFLINE = " (%s was offline)";

  public static final String SKIPPED_IDLE = " (%s was idle)";

  public static final String BANNED_MAP = " (%s was playing a banned beatmap)";

  public static final String SKIPPED_FORCE = "%s was skipped by %s.";

  public static final String SPECTATE_FORCE = "%s has force spectated %s.";

  public static final String TIMEOUT = "%s has been timed out for %d minutes.";

  public static final String ADDED_BANNED_MAPS_FILTER = "Filter added.";
  
  public static final String BOOST_QUEUE = "%s was boosted to the front of the queue by %s.";
  
  public static final String UNTRUSTED = "Your twitch account has been flagged as suspicious"
      + " by a routine check. This is no cause for concern, but we kindly ask you to link"
      + " your twitch account to you osu! account using the !link command."
      + " We apologize for the inconvience.";

  public static final String LINK_INSTRUCTIONS = "To link your account, please login to osu!"
      + " and send the following command to the player OsuCelebrity: !link %s";
  
  public static final String NEW_SPECTATEE = "NOW SPECTATING: %s";
  
  public static final String OLD_SPECTATEE = " | Thanks for playing, %s!";
}
