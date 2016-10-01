package me.reddev.osucelebrity;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.ws.rs.ServerErrorException;

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
  public static void handleException(Logger log, Exception ex,
      @CheckForNull Consumer<String> messager) {
    try {
      throw ex;
    } catch (UserException e) {
      if (messager != null) {
        // no need to log this. send to user.
        messager.accept(e.getMessage());
      } else {
        // wherever there can be a userexception, we expect a user
        log.error("unexpected UserException", e);
      }
    } catch (SocketTimeoutException | ServerErrorException e) {
      log.debug("API timeout.", e);
      if (messager != null) {
        messager.accept(Responses.EXCEPTION_TIMEOUT);
      }
    } catch (IOException e) {
      log.error("API exception.", e);
      if (messager != null) {
        messager.accept(Responses.EXCEPTION_IO);
      }
    } catch (Exception e) {
      log.error("exception", e);
      if (messager != null) {
        messager.accept(Responses.EXCEPTION_INTERNAL);
      }
    }
  }
}
