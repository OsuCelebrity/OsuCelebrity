package me.reddev.osucelebrity.twitch;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Websocket client to connect to the OBS plugin OBS Remote.
 */
@Slf4j
@WebSocket(maxTextMessageSize = 64 * 1024)
public class ObsRemote implements SceneSwitcher {

  /*
   * used to block the connect call until the connection is closed.
   */
  private CountDownLatch disconnectLatch;
  
  private CountDownLatch connectLatch = new CountDownLatch(1);

  private Session session;

  /**
   * Handler for disconnect event.
   */
  @OnWebSocketClose
  public synchronized void onDisconnect(int statusCode, String reason) {
    log.debug("Disconnected from OBS Remote: {}, {}", statusCode, reason);
    this.session = null;
    this.disconnectLatch.countDown();
  }

  /**
   * Handler for connect event.
   */
  @OnWebSocketConnect
  public synchronized void onConnect(Session session) throws IOException {
    log.debug("Connected to OBS Remote");
    this.session = session;
    connectLatch.countDown();
  }
  
  @Override
  public void awaitConnect() throws InterruptedException {
    connectLatch.await();
  }

  @OnWebSocketMessage
  public void onMessage(String msg) {
    log.debug("Received from OBS Remote: {}", msg);
  }

  @Override
  public synchronized void changeScene(String sceneName) throws IOException {
    ensureConnected();
    session.getRemote().sendString(String.format(CHANGE_SCENE, sceneName, messageCounter++));
  }

  void ensureConnected() {
    if (session == null) {
      throw new IllegalStateException("Not connected to OBS Remote.");
    }
  }

  int messageCounter = 0;

  static final String CHANGE_SCENE =
      "{\"request-type\":\"SetCurrentScene\",\"scene-name\":\"%s\",\"message-id\":\"%d\"}";

  static final String OBS_REMOTE_ADDR = "ws://localhost:4444/";

  /**
   * Connects to OBS Remote and blocks until the connection drops. Logs Exceptions.
   */
  @Override
  public void connect() {
    disconnectLatch = new CountDownLatch(1);
    WebSocketClient client = new WebSocketClient();
    /*
     * This idle timeout should be working since OBS Remote spams status updates.
     */
    client.setMaxIdleTimeout(120000);
    ClientUpgradeRequest request = new ClientUpgradeRequest();
    request.setSubProtocols("obsapi");
    try {
      client.start();
      log.debug("Connecting to OBS Remote");
      try {
        client.connect(this, new URI(OBS_REMOTE_ADDR), request).get(5, TimeUnit.SECONDS);
      } catch (ExecutionException e) {
        throw e.getCause();
      }
      disconnectLatch.await();
    } catch (ConnectException e) {
      log.error("Cannot connect to OBS Remote", e);
    } catch (Throwable e) {
      log.error("exception in connection to OBS Remote", e);
    } finally {
      try {
        client.stop();
      } catch (Exception e) {
        log.error("exception while closing connection to OBS Remote", e);
      }
    }
  }
  
  public static void main(String[] args) {
    new ObsRemote().connect();
  }
}
