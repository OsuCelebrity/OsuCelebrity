package me.reddev.osucelebrity;

import static org.junit.Assert.*;

import org.junit.Test;


public class CommandsTest {
  @Test
  public void testWhitespace() throws Exception {
    assertTrue(Commands.QUEUE.endsWith(" "));
    assertTrue(Commands.FORCESKIP.endsWith(" "));
  }
}
