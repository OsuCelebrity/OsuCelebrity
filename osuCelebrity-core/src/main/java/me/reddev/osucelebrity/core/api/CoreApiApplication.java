package me.reddev.osucelebrity.core.api;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.Application;

/**
 * jetty Application for the internal JSON api.
 */
public class CoreApiApplication extends Application {
  final Set<Object> resourceInstances = new HashSet<>();

  /**
   * Constructor.
   */
  @Inject
  public CoreApiApplication(CurrentPlayerService currentPlayerService, QueueService queue) {
    super();

    resourceInstances.add(currentPlayerService);
    resourceInstances.add(queue);
  }

  @Override
  public Set<Object> getSingletons() {
    return resourceInstances;
  }

}
