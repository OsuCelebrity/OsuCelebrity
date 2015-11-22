package me.reddev.osucelebrity.core.api;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.Spectator;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/queue")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class QueueService {
  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;

  /**
   * Shows the current queue top.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<DisplayQueuePlayer> queue() {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      return spectator.getCurrentQueue(pm);
    } catch (Exception e) {
      throw e;
    } finally {
      pm.close();
    }
  }
}
