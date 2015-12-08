package me.reddev.osucelebrity;

import org.apache.commons.lang3.StringUtils;

public class Commands {
  public static final String FORCESKIP = "forceskip ";
  public static final String[] QUEUE = { "spec ", "vote ", "give " };
  public static final String SELFQUEUE = "spec";
  public static final String OPTIN = "optin";
  public static final String OPTOUT = "optout";
  public static final String UNMUTE = "unmute";
  public static final String MUTE = "mute";
  public static final String DOWNVOTE = "skip";
  public static final String UPVOTE = "dank";
  public static final String FORCESPEC = "forcespec ";
  public static final String POSITION = "position ";
  public static final String SELFPOSITION = "position";
  public static final String GAME_MODE = "gamemode ";
  public static final String NOW_PLAYING = "np";
  public static final String RESTART_CLIENT = "fix";
  public static final String MOD = "mod ";
  public static final String BOOST = "boost ";
  public static final String TIMEOUT = "timeout ";
  public static final String ADD_BANNED_MAPS_FILTER = "banmaps ";
  public static final String EXTEND = "extend ";
  
  /**
   * Detects a command in an incoming message.
   * @param command the whole message
   * @param candidates candidate commands
   * @return null of no candidate was found, or the remaining string if a candidate was found.
   */
  public static String detect(String command, String... candidates) {
    for (String token : candidates) {
      if (StringUtils.startsWithIgnoreCase(command, token)) {
        return command.substring(token.length());
      }
    }
    return null;
  }
}
