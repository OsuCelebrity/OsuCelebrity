package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.osu.OsuStatus.Type;

import me.reddev.osucelebrity.osu.OsuStatus;



public interface StatusWindow {
  public static class DummyStatusWindow implements StatusWindow {
    @Override
    public void setStatus(Type type) {}

    @Override
    public void newPlayer() {}

    @Override
    public void setApproval(double approval) {}

    @Override
    public void setRemainingTime(long remainingTime) {}
  }

  /**
   * Reports the current osu client status.
   */
  public abstract void setStatus(OsuStatus.Type type);

  /**
   * Reports that we started to spectate a new player.
   */
  public abstract void newPlayer();

  /**
   * Reports the current approval.
   */
  public abstract void setApproval(double approval);

  /**
   * Reports the remaining time for the current player.
   */
  public abstract void setRemainingTime(long remainingTime);
}
