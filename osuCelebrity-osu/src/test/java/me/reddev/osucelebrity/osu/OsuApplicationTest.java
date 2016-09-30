package me.reddev.osucelebrity.osu;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import me.reddev.osucelebrity.osu.OsuApplication.OsuApplicationSettings;
import me.reddev.osucelebrity.osu.OsuStatus.Type;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class OsuApplicationTest {
  @Mock
  OsuApplicationSettings settings;

  OsuApplication osuApp = new OsuApplication(null);

  @Test
  public void testWindowTitleWatchingEdge() throws Exception {
    osuApp.windowTitle = "osu!cuttingedge b20160304 -  (watching Angelsim)";
    assertEquals(new OsuStatus(Type.WATCHING, "Angelsim"), osuApp.getStatus());
  }

  @Test
  public void testWindowTitleWatching() throws Exception {
    osuApp.windowTitle = "osu!  -  (watching hvick225)";
    assertEquals(new OsuStatus(Type.WATCHING, "hvick225"), osuApp.getStatus());
  }

  @Test
  public void testWindowTitlePlaying() throws Exception {
    osuApp.windowTitle = "osu!  - Ni-Sokkususu - Shukusai no Elementalia [GAPS 'n' JUMPS!]";
    assertEquals(new OsuStatus(Type.PLAYING,
        "Ni-Sokkususu - Shukusai no Elementalia [GAPS 'n' JUMPS!]"), osuApp.getStatus());
  }

  public static void main(String[] args) throws Exception {
    System.out.println(new OsuApplication(null).readWindowTitle());
  }
}
