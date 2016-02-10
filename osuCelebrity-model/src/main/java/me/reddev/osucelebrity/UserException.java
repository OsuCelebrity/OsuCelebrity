package me.reddev.osucelebrity;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

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

  public UserException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Log an exception and provide feedback to the user.
   * 
   * @param log the logger to log to
   * @param ex the exception that occurred
   * @param messager how to reach the user
   */
  public static void handleException(Logger log, Exception ex, Consumer<String> messager) {
    try {
      throw ex;
    } catch (UserException e) {
      // no need to log this
      messager.accept(e.getMessage());
    } catch (SocketTimeoutException e) {
      log.debug("API timeout.", e);
      messager.accept(Responses.EXCEPTION_TIMEOUT);
    } catch (IOException e) {
      log.error("API exception.", e);
      messager.accept(Responses.EXCEPTION_IO);
    } catch (Exception e) {
      log.error("exception", e);
      messager.accept(Responses.EXCEPTION_INTERNAL);
    }
  }
}
