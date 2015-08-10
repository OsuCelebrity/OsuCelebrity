package me.reddev.osucelebrity.osu.commands;

import me.reddev.osucelebrity.osu.AbstractOsuCommand;
import me.reddev.osucelebrity.osu.Osu;
import org.tillerino.osuApiModel.OsuApiUser;

public class QueueSelfOsuCommandImpl extends AbstractOsuCommand implements QueueSelfOsuCommand {
  public QueueSelfOsuCommandImpl(Osu osu, OsuApiUser user) {
    super(osu, user);
  }
}
