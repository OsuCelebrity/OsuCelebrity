package me.reddev.osucelebrity.osu;

import org.pircbotx.PircBotX;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.pircbotx.Utils;
import org.pircbotx.hooks.events.UnknownEvent;

public class Pinger {
  volatile String pingMessage = null;
  volatile CountDownLatch pingLatch = null;

  /*
   * this method is synchronized externally
   */
  void ping(PircBotX bot) throws IOException, InterruptedException {
    if (!bot.isConnected()) {
      throw new IOException("bot is no longer connected");
    }

    synchronized (this) {
      pingLatch = new CountDownLatch(1);
      pingMessage = UUID.randomUUID().toString();
    }

    Utils.sendRawLineToServer(bot, "PING " + pingMessage);

    if (!pingLatch.await(10, TimeUnit.SECONDS)) {
      throw new IOException("ping timed out");
    }
  }

  void handleUnknownEvent(@SuppressWarnings("rawtypes") UnknownEvent event) {
    synchronized (this) {
      if (pingMessage == null) {
        return;
      }

      boolean contains = event.getLine().contains(" PONG ");
      boolean endsWith = event.getLine().endsWith(pingMessage);
      if (contains && endsWith) {
        pingLatch.countDown();
      }
    }
  }
}