package me.reddev.osucelebrity.core.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;

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
  }

  private final PersistenceManagerFactory pmf;
  
  private final Spectator spectator;

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
      currentPlayer.setName(player.getPlayer().getUserName());
      return currentPlayer;
    } finally {
      pm.close();
    }
  }
}
