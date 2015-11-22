package me.reddev.osucelebrity.core.api;

import static me.reddev.osucelebrity.core.api.CurrentPlayerService.formatDuration;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CurrentPlayerServiceTest {

  @Test
  public void testFormatDuration() throws Exception {
    assertEquals("0:00", formatDuration(0));
    assertEquals("0:59", formatDuration(59000));
    assertEquals("1:02", formatDuration(62000));
    assertEquals("1:02:03", formatDuration(3723000));
  }

}
