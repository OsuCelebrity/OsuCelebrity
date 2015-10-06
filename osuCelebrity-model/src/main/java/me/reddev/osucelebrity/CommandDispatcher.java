package me.reddev.osucelebrity;

import java.util.ArrayList;
import java.util.List;

/**
 * Able to dispatch commands from the command handlers.
 * @author Redback
 *
 * @param <T> The command type.
 */
public class CommandDispatcher<T> { 
  List<CommandHandler<T>> commandHandlers = new ArrayList<CommandHandler<T>>();
  
  /**
   * Dispatches a new command to the handlers.
   * @param command The command to dispatch.
   * @throws Exception If any handler throws an exception.
   */
  public void dispatchCommand(T command) throws Exception {
    for (CommandHandler<T> commandHandler : commandHandlers) {
      if (commandHandler.handle(command)) {
        return;
      }
    }
  }

  public void addHandler(CommandHandler<T> handler) {
    commandHandlers.add(handler);
  }
}
