package me.reddev.osucelebrity.twitch;

public interface TwitchCommandHandler {
  /**
   * @param command a command
   * @return true if the command was handled
   * @throws Exception an exception implies that this handler was supposed to handle the given
   *         command, i.e. no other handlers need to be invoked
   */
  public boolean handle(TwitchCommand command) throws Exception;
}
