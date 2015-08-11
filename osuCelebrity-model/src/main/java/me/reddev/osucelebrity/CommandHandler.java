package me.reddev.osucelebrity;

/**
 * Defines a handler of commands.
 * @author Redback
 *
 * @param <T> The command type
 */
public interface CommandHandler<T> {
  boolean handle(T command) throws Exception;
}
