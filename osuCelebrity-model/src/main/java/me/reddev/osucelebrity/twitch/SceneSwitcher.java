package me.reddev.osucelebrity.twitch;

import java.io.IOException;

/**
 * Controls our broadcasting software.
 */
public interface SceneSwitcher {
  /**
   * Switches to the given scene.
   * 
   * @param sceneName name of the new scene.
   */
  public abstract void changeScene(String sceneName) throws IOException;
  
  /**
   * If necessary, keeps a connection open. May block indefinitely.
   */
  public void connect();
  
  /**
   * Blocks until a connection is established, if that is necessary.
   */
  public void awaitConnect() throws InterruptedException;
}
