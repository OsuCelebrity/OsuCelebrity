package me.reddev.osucelebrity;

/**
 * These exceptions are caused by user input. The message will be passed on to the user.
 * 
 * @author Tillerino
 */
public class UserException extends Exception {
  private static final long serialVersionUID = 1L;

  public UserException(String message) {
    super(message);
  }
}
