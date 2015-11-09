package me.reddev.osucelebrity.osu;

public interface Osu {
  /**
   * Starts spectating a player. TODO specify behaviour (async with callback? success/failure?)
   * 
   * @param user The user to be spectated.
   */
  public void startSpectate(OsuUser user);

  /**
   * Registers a command handler. The order of these handlers is preserved and every command is
   * passed to every handler in the order in which they were registered until on handler's
   * {@link OsuCommandHandler#handle(OsuCommand)} method returns true.
   * 
   * @param handler handler
   */
  public void registerCommandHandler(OsuCommandHandler handler);

  /**
   * Sends a message via ingame chat.
   * 
   * @param osuUser recipient
   * @param message message
   */
  public void message(OsuUser osuUser, String message);

  /**
   * Notifies a player about their upcoming spectate period.
   * 
   * @param user Recipient
   */
  public void notifyStarting(OsuUser user);

  /**
   * Notifies a player that they're going to be spectated soon.
   * 
   * @param osuUser the player to be notified.
   */
  public void notifySoon(OsuUser osuUser);
}
