package me.reddev.osucelebrity.core;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.QueueUser;
import me.reddev.osucelebrity.QueueUser.QueueSource;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuCommand;
import me.reddev.osucelebrity.osu.commands.QueueSelfOsuCommand;
import me.reddev.osucelebrity.twitch.Twitch;
import me.reddev.osucelebrity.twitch.TwitchCommand;
import me.reddev.osucelebrity.twitch.commands.NextUserTwitchCommand;
import me.reddev.osucelebrity.twitch.commands.QueueUserTwitchCommand;
import org.tillerino.osuApiModel.OsuApiUser;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Connects to osu and twitch and manages the queue. Please break up this class as it grows!
 * 
 * @author Tillerino
 */
@Slf4j
public class Core implements Runnable {
  final Osu osu;
  final Twitch twitch;
  final CoreSettings settings;

  final BlockingQueue<QueueUser> queue = new LinkedBlockingQueue<>();

  /**
   * Creates a osuCelebrity core object.
   * @param osu A controller for osu! Irc and Application
   * @param twitch A controller for Twitch Irc
   * @param settings The settings for the core program
   */
  public Core(Osu osu, Twitch twitch, CoreSettings settings) {
    super();
    this.osu = osu;
    this.twitch = twitch;
    this.settings = settings;

    osu.registerCommandHandler(this::handleOsuCommand);
    twitch.registerCommandHandler(this::handleTwitchCommand);
  }
  
  boolean handleOsuCommand(OsuCommand command) throws Exception {
    if (command instanceof QueueSelfOsuCommand) {
      QueueSelfOsuCommand queueCommand = (QueueSelfOsuCommand) command;
      if (!queued(queueCommand.getUser())) {
        queue.add(new QueueUser(queueCommand.getUser(), QueueSource.OSU));
        log.info("Queued " + queueCommand.getUser().getUserName());
        osu.message(queueCommand.getUser(), String.format(OsuResponses.ADDED_TO_QUEUE));
        return true;
      }
    }
    return false;
  }
  
  boolean handleTwitchCommand(TwitchCommand command) throws Exception {
    if (command instanceof QueueUserTwitchCommand) {
      QueueUserTwitchCommand queueCommand = (QueueUserTwitchCommand) command;
      if (!queued(queueCommand.getRequestUser())) {
        queue.add(new QueueUser(queueCommand.getRequestUser(), QueueSource.TWITCH));
        log.info("Queued " + queueCommand.getRequestUser().getUserName());
        twitch.sendMessageToChannel(String.format(TwitchResponses.ADDED_TO_QUEUE, 
            queueCommand.getRequestUser().getUserName()));
        return true;
      }
    } else if (command instanceof NextUserTwitchCommand) {
      QueueUser next = queue.peek(); 
      if (next != null) {
        twitch.sendMessageToChannel(String.format(TwitchResponses.NEXT_IN_QUEUE, 
            next.getQueuedPlayer().getUserName()));
      } else {
        twitch.sendMessageToChannel(String.format(TwitchResponses.QUEUE_EMPTY));
      }
      return true;
    }
    return false;
  }
  
  /**
   * Checks whether the queue contains a user.
   * @return True if the queue contains the user (any source)
   */
  @SuppressFBWarnings("GC")
  boolean queued(OsuApiUser user) {
    //Since FindBugs doesn't know I've overwritten the equals method for QueueUser
    return queue.contains(user);
  }

  @Override
  public void run() {
    for (long spectatingSince = 0;;spectatingSince = System.currentTimeMillis()) {
      final QueueUser nextUser;
      try {
        nextUser = queue.take();
        if (nextUser.getQueueSource() == QueueSource.OSU) {
          osu.notifyUpcoming(nextUser.getQueuedPlayer());
        }
      } catch (InterruptedException e) {
        return;
      }
      long time = System.currentTimeMillis();
      while (time < spectatingSince + settings.getDefaultSpecDuration()) {
        try {
          Thread.sleep(spectatingSince + settings.getDefaultSpecDuration() - time);
        } catch (InterruptedException e) {
          return;
        }
      }
      osu.startSpectate(nextUser.getQueuedPlayer());
    }
  }
}
