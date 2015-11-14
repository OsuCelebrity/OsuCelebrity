package me.reddev.osucelebrity.core.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/current")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class CurrentPlayerService {
  @Data
  public static class CurrentPlayer {
    String name;
    
    int id;

    String playingFor;
    
    double health;
    
    String beatmap;
  }

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;
  
  private final CoreSettings coreSettings;
  
  private final Osu osu;

  /**
   * Shows details about the player currently being spectated.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public CurrentPlayer current() {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      CurrentPlayer currentPlayer = new CurrentPlayer();
      QueuedPlayer player = spectator.getCurrentPlayer(pm);
      if (player != null) {
        currentPlayer.setName(player.getPlayer().getUserName());
        {
          Duration duration = Duration.ofMillis(System.currentTimeMillis() - player.getStartedAt());
          LocalTime localTime = LocalTime.MIDNIGHT.plus(duration);
          currentPlayer.setPlayingFor(DateTimeFormatter.ofPattern("m:ss").format(localTime));
        }
        long timeLeft = Math.max(0, player.getStoppingAt() - player.getLastRemainingTimeUpdate());
        currentPlayer.setHealth(timeLeft
            / (double) coreSettings.getDefaultSpecDuration());
        currentPlayer.setId(player.getPlayer().getUserId());
      }
      OsuStatus clientStatus = osu.getClientStatus();
      if (clientStatus != null && clientStatus.getType() == Type.PLAYING) {
        currentPlayer.setBeatmap(clientStatus.getDetail());
      }
      return currentPlayer;
    } finally {
      pm.close();
    }
  }
}
