package me.reddev.osucelebrity;

import static org.junit.Assert.*;

import org.junit.Test;


public class CommandsTest {
  @Test
  public void testWhitespace() throws Exception {
    for (String token : Commands.QUEUE) {
      assertTrue(token.endsWith(" "));
    }
    assertTrue(Commands.FORCESKIP.endsWith(" "));
  }
}
