package me.reddev.osucelebrity.twitch;

import me.reddev.osucelebrity.PassAndReturnNonnull;
import me.reddev.osucelebrity.core.SkipReason;
import me.reddev.osucelebrity.osu.OsuUser;

@PassAndReturnNonnull
public interface Twitch {
  public void whisperUser(String nick, String message);

  public void announcePlayerSkipped(SkipReason reason, OsuUser player);
}
