package me.reddev.osucelebrity.util;

import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

public class ExecutorServiceHelper {
  public interface Async {
    public void run() throws Exception;
  }
  
  public interface AsyncConsumer<T> {
    public void run(T arg) throws Exception;
  }
  
  public interface AsyncBiConsumer<T, U> {
    public void run(T arg1, U arg2) throws Exception;
  }
  
  public interface AsyncTerConsumer<T, U, V> {
    public void run(T arg1, U arg2, V arg3) throws Exception;
  }
  
  static void logExceptions(Logger log, Async async) {
    try {
      async.run();
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }
  
  static <T> void logExceptions(Logger log, AsyncConsumer<T> async, T arg) {
    try {
      async.run(arg);
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }
  
  static <T, U> void logExceptions(Logger log, AsyncBiConsumer<T, U> async, T arg1, U arg2) {
    try {
      async.run(arg1, arg2);
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }
  
  static <T, U, V> void logExceptions(Logger log, AsyncTerConsumer<T, U, V> async, T arg1, U arg2,
      V arg3) {
    try {
      async.run(arg1, arg2, arg3);
    } catch (Exception e) {
      log.error("Exception", e);
    }
  }
  
  public static void schedule(ExecutorService exec, Logger log, Async async) {
    exec.submit(() -> logExceptions(log, async));
  }
  
  static <T> T detachIfPossible(PersistenceManager pm, T arg) {
    if (JDOHelper.isPersistent(arg) && !JDOHelper.isDetached(arg)) {
      return pm.detachCopy(arg);
    } else {
      return arg;
    }
  }
  
  /**
   * Detaches any persistend and non-detached objects and schedules the async to run with the
   * detached objects.
   * 
   * @param exec the {@link ExecutorService} to schedule the async on
   * @param log log for exceptions
   * @param pm the persistence manager to detach the objects from
   * @param async the async to be executed
   * @param arg object to pass to the async
   */
  public static <T> void detachAndSchedule(ExecutorService exec, Logger log, PersistenceManager pm,
      AsyncConsumer<T> async, T arg) {
    T detachedArg = detachIfPossible(pm, arg);
    exec.submit(() -> logExceptions(log, async, detachedArg));
  }

  /**
   * Detaches any persistend and non-detached objects and schedules the async to run with the
   * detached objects.
   * 
   * @param exec the {@link ExecutorService} to schedule the async on
   * @param log log for exceptions
   * @param pm the persistence manager to detach the objects from
   * @param async the async to be executed
   * @param arg1 object to pass to the async
   * @param arg2 object to pass to the async
   */
  public static <T, U> void detachAndSchedule(ExecutorService exec, Logger log,
      PersistenceManager pm, AsyncBiConsumer<T, U> async, T arg1, U arg2) {
    T detachedArg1 = detachIfPossible(pm, arg1);
    U detachedArg2 = detachIfPossible(pm, arg2);
    exec.submit(() -> logExceptions(log, async, detachedArg1,
        detachedArg2));
  }

  /**
   * Detaches any persistend and non-detached objects and schedules the async to run with the
   * detached objects.
   * 
   * @param exec the {@link ExecutorService} to schedule the async on
   * @param log log for exceptions
   * @param pm the persistence manager to detach the objects from
   * @param async the async to be executed
   * @param arg1 object to pass to the async
   * @param arg2 object to pass to the async
   */
  public static <T, U, V> void detachAndSchedule(ExecutorService exec, Logger log,
      PersistenceManager pm, AsyncTerConsumer<T, U, V> async, T arg1, U arg2, V arg3) {
    T detachedArg1 = detachIfPossible(pm, arg1);
    U detachedArg2 = detachIfPossible(pm, arg2);
    V detachedArg3 = detachIfPossible(pm, arg3);
    exec.submit(() -> logExceptions(log, async, detachedArg1,
        detachedArg2, detachedArg3));
  }
}
