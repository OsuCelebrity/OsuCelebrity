package me.reddev.osucelebrity.twitch;

public interface Twitch {
  /**
   * Registers a command handler. The order of these handlers is preserved and every command is
   * passed to every handler in the order in which they were registered until on handler's
   * {@link TwitchCommandHandler#handle(TwitchCommand)} method returns true.
   * 
   * @param handler handler
   */

  public void sendMessageToChannel(String message);

  public void whisperUser(String nick, String message);
}
