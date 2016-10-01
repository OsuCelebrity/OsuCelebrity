package me.reddev.osucelebrity;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public abstract class AbstractIrcBot extends ListenerAdapter<PircBotX> implements Runnable {
  public static class PublicSocketBot extends PircBotX {
    public PublicSocketBot(Configuration<? extends PircBotX> configuration) {
      super(configuration);
    }
    
    @Override
    public Socket getSocket() {
      return super.getSocket();
    }
  }

  private PublicSocketBot bot;
  
  protected abstract Configuration<PircBotX> getConfiguration() throws Exception;
  
  protected abstract Logger getLog();
  
  private final CountDownLatch connectLatch = new CountDownLatch(1);
  
  /**
   * Creates a bot instance and connects.
   */
  public void run() {
    try {
      bot = new PublicSocketBot(getConfiguration());
      
      getLog().debug("Connecting to {} as {}", bot.getConfiguration().getServerHostname(),
          bot.getConfiguration().getLogin());
      bot.startBot();
    } catch (Exception e) {
      getLog().error("IRC error", e);
    }
  }
  
  public PublicSocketBot getBot() {
    return bot;
  }

  /**
   * Disconnects from the IRC server.
   */
  public void forceDisconnect() {
    try {
      bot.getSocket().close();
    } catch (IOException e) {
      getLog().error("exception while disconnecting osu IRC bot", e);
    }
  }
  
  public boolean isConnected() {
    Socket socket = bot.getSocket();
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  @Override
  public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
    connectLatch.countDown();
    getLog().debug("Connected to {} as {}", bot.getConfiguration().getServerHostname(),
          bot.getConfiguration().getLogin());
  }

  @Override
  public void onDisconnect(DisconnectEvent<PircBotX> event) throws Exception {
    getLog().debug("Disconnected from {}", bot.getConfiguration().getServerHostname());
  }
  
  public void awaitConnect() throws InterruptedException {
    connectLatch.await();
  }
}
