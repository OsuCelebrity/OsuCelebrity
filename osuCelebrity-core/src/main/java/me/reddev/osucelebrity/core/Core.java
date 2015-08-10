package me.reddev.osucelebrity.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.tillerino.osuApiModel.OsuApiUser;

import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuCommand;
import me.reddev.osucelebrity.osu.commands.QueueSelfOsuCommand;
import me.reddev.osucelebrity.twitch.Twitch;

/**
 * Connects to osu and twitch and manages the queue. Please break up this class as it grows!
 * 
 * @author Tillerino
 */
public class Core implements Runnable {
  final Osu osu;
  final Twitch twitch;
  final CoreSettings settings;

  final BlockingQueue<OsuApiUser> queue = new LinkedBlockingQueue<>();

  public Core(Osu osu, Twitch twitch, CoreSettings settings) {
    super();
    this.osu = osu;
    this.twitch = twitch;
    this.settings = settings;

    osu.registerCommandHandler(this::handleOsuCommand);
  }

  boolean handleOsuCommand(OsuCommand command) {
    if (command instanceof QueueSelfOsuCommand) {
      if (!queue.contains(command.getUser())) {
        queue.add(command.getUser());
      }
    }
    return false;
  }

  @Override
  public void run() {
    for (long spectatingSince = 0;;spectatingSince = System.currentTimeMillis()) {
      final OsuApiUser nextUser;
      try {
        nextUser = queue.take();
      } catch (InterruptedException e) {
        return;
      }
      long time = System.currentTimeMillis();
      while(time < spectatingSince + settings.getDefaultSpecDuration()) {
        try {
          Thread.sleep(spectatingSince + settings.getDefaultSpecDuration() - time);
        } catch (InterruptedException e) {
          return;
        }
      }
      osu.startSpectate(nextUser);
    }
  }
}
