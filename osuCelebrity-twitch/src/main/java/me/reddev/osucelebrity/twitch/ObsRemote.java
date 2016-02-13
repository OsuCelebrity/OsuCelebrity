package me.reddev.osucelebrity.twitch;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

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
  public void connect() {
    disconnectLatch = new CountDownLatch(1);
    WebSocketClient client = new WebSocketClient();
    ClientUpgradeRequest request = new ClientUpgradeRequest();
    request.setSubProtocols("obsapi");
    try {
      client.start();
      log.debug("Connecting to OBS Remote");
      client.connect(this, new URI(OBS_REMOTE_ADDR), request);
      disconnectLatch.await();
    } catch (Exception e) {
      log.error("exception in connection to OBS Remote", e);
    } finally {
      try {
        client.stop();
      } catch (Exception e) {
        log.error("exception while closing connection to OBS Remote", e);
      }
    }
  }
}
