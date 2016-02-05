package me.reddev.osucelebrity.core;

import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;

import java.util.List;

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

    @Override
    public void setTwitchMods(List<String> mods) {}

    @Override
    public void setQueue(List<QueuedPlayer> queue) {}

    @Override
    public void setRawApproval(double approval) {}

    @Override
    public void setFrozen(boolean frozen) {}
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
   * Reports the raw approval (without time penalty).
   */
  public abstract void setRawApproval(double approval);

  /**
   * Reports the remaining time for the current player.
   */
  public abstract void setRemainingTime(long remainingTime);

  /**
   * Reports the current mods in the twitch channel.
   */
  public void setTwitchMods(List<String> mods);

  /**
   * Reports the current queue.
   */
  public void setQueue(List<QueuedPlayer> queue);

  /**
   * Reports whether the spectator is frozen (see {@link Spectator#setFrozen(boolean)}).
   */
  public abstract void setFrozen(boolean frozen);
}
