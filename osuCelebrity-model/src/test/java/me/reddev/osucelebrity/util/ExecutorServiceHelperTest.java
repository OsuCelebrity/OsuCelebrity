package me.reddev.osucelebrity.util;

import org.mockito.Mock;
import me.reddev.osucelebrity.util.ExecutorServiceHelper.Async;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import org.slf4j.Logger;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.AbstractJDOTest;
import org.junit.Test;

import javax.jdo.JDOHelper;

public class ExecutorServiceHelperTest extends AbstractJDOTest {
  @Mock
  Logger log;

  @Test
  public void testOneArg() throws Exception {
    AtomicReference<OsuUser> ref = new AtomicReference<>();

    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, ref::set, osuUser);

    assertTrue(JDOHelper.isDetached(ref.get()));
  }

  @Test
  public void testOneArgNotPersistent() throws Exception {
    AtomicReference<Object> ref = new AtomicReference<>();

    Object object = new Object();
    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, ref::set, object);

    assertSame(object, ref.get());
  }

  @Test
  public void testOneArgDetached() throws Exception {
    AtomicReference<OsuUser> ref = new AtomicReference<>();

    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, ref::set, pm.detachCopy(osuUser));
  }

  @Test
  public void testOneArgException() throws Exception {
    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, x -> {
      throw new Exception();
    }, osuUser);

    verify(log).error(anyString(), any(Exception.class));
  }

  @Test
  public void test() throws Exception {
    Async async = mock(Async.class);

    ExecutorServiceHelper.schedule(exec, log, async);

    verify(async).run();
  }

  @Test
  public void testException() throws Exception {
    ExecutorServiceHelper.schedule(exec, log, () -> {
      throw new Exception();
    });

    verify(log).error(anyString(), any(Exception.class));
  }

  @Test
  public void testTwoArg() throws Exception {
    AtomicReference<OsuUser> ref1 = new AtomicReference<>();
    AtomicReference<OsuUser> ref2 = new AtomicReference<>();

    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, (x, y) -> {
      ref1.set(x);
      ref2.set(y);
    }, osuUser, osuUser2);

    assertTrue(JDOHelper.isDetached(ref1.get()));
    assertTrue(JDOHelper.isDetached(ref2.get()));
  }

  @Test
  public void testTwoArgException() throws Exception {
    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, (x, y) -> {
      throw new Exception();
    }, null, null);

    verify(log).error(anyString(), any(Exception.class));
  }

  @Test
  public void testThreeArg() throws Exception {
    AtomicReference<OsuUser> ref1 = new AtomicReference<>();
    AtomicReference<OsuUser> ref2 = new AtomicReference<>();
    AtomicReference<OsuUser> ref3 = new AtomicReference<>();

    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, (x, y, z) -> {
      ref1.set(x);
      ref2.set(y);
      ref3.set(z);
    }, osuUser, osuUser2, osuUser3);

    assertTrue(JDOHelper.isDetached(ref1.get()));
    assertTrue(JDOHelper.isDetached(ref2.get()));
    assertTrue(JDOHelper.isDetached(ref3.get()));
  }

  @Test
  public void testThreeArgException() throws Exception {
    ExecutorServiceHelper.detachAndSchedule(exec, log, pm, (x, y, z) -> {
      throw new Exception();
    }, null, null, null);

    verify(log).error(anyString(), any(Exception.class));
  }

}
