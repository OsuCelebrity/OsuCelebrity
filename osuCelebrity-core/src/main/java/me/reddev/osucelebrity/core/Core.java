package me.reddev.osucelebrity.core;

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
  
  final Spectator spectator;

  /**
   * Creates a osuCelebrity core object.
   * @param osu A controller for osu! Irc and Application
   * @param twitch A controller for Twitch Irc
   * @param settings The settings for the core program
   */
  public Core(Osu osu, Twitch twitch, CoreSettings settings, Spectator spectator) {
    super();
    this.osu = osu;
    this.twitch = twitch;
    this.settings = settings;
    this.spectator = spectator;

    osu.registerCommandHandler(this::handleOsuCommand);
    twitch.registerCommandHandler(this::handleTwitchCommand);
  }
  
  boolean handleOsuCommand(OsuCommand command) throws Exception {
    if (command instanceof QueueSelfOsuCommand) {
      QueueSelfOsuCommand queueCommand = (QueueSelfOsuCommand) command;
      if (spectator.enqueue(new QueueUser(command.getUser(), QueueSource.OSU))) {
        osu.message(queueCommand.getUser(), String.format(OsuResponses.ADDED_TO_QUEUE));
      }
      return true;
    }
    return false;
  }
  
  boolean handleTwitchCommand(TwitchCommand command) throws Exception {
    if (command instanceof QueueUserTwitchCommand) {
      QueueUserTwitchCommand queueCommand = (QueueUserTwitchCommand) command;
      if (spectator.enqueue(new QueueUser(queueCommand.getRequestUser(), QueueSource.TWITCH))) {
        twitch.sendMessageToChannel(String.format(TwitchResponses.ADDED_TO_QUEUE, 
            queueCommand.getRequestUser().getUserName()));
      }
      return true;
    } else if (command instanceof NextUserTwitchCommand) {
      if (!spectator.advance()) {
        twitch.sendMessageToChannel(String.format(TwitchResponses.QUEUE_EMPTY));
      }
      return true;
    }
    return false;
  }

  @Override
  public void run() {
    spectator.run();
  }
}
