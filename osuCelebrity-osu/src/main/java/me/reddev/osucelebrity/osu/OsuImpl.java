package me.reddev.osucelebrity.osu;

import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuImpl implements Osu {
  private final OsuIrcBot bot;
  private final OsuApplication app;
  private final Spectator spectator;
  private final PersistenceManagerFactory pmf;

  @Override
  public void startSpectate(OsuUser user) {
    try {
      app.spectate(user);
    } catch (IOException e) {
      log.error("error spectating", e);
    }
  }

  @Override
  public void message(OsuUser user, String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void notifyStarting(OsuUser player) {
    bot.messagePlayer(player, String.format(OsuResponses.SPECTATING_NOW));
  }
  
  @Override
  public void notifyNext(OsuUser player) {
    bot.messagePlayer(player, String.format(OsuResponses.SPECTATING_NEXT));
  }
  
  @Override
  public void notifyDone(OsuUser player) {
    bot.messagePlayer(player, String.format(OsuResponses.DONE_SPECTATING));
  }
  
  @Override
  public void notifyQueued(OsuUser player, int queuePosition) {
    bot.messagePlayer(player, String.format(OsuResponses.QUEUED, queuePosition));
  }
  
  @Override
  public void notifyStatistics(OsuUser player, long danks, long skips) {
    bot.messagePlayer(player, String.format(OsuResponses.STATISTICS, danks, skips));
  }

  @Override
  public OsuStatus getClientStatus() {
    return app.getStatus();
  }

  @Override
  public List<String> getOnlineUsers() {
    return new ArrayList<>(bot.getOnlineUsers());
  }

  @Override
  public boolean isOnline(OsuUser player) {
    return bot.getOnlineUsers().contains(player.getUserName().replace(' ', '_'));
  }

  @Override
  public long lastActivity(PersistenceManager pm, OsuUser player) {
    try (JDOQuery<PlayerActivity> query =
        new JDOQuery<PlayerActivity>(pm).select(playerActivity).from(playerActivity)
            .where(playerActivity.user.userId.eq(player.getUserId()))
            .orderBy(playerActivity.lastActivity.desc())) {
      PlayerActivity activity = query.fetchFirst();
      if (activity != null) {
        return activity.getLastActivity();
      }
      return 0;
    }
  }
  
  @Override
  public void pollIngameStatus(OsuUser player) {
    bot.pollIngameStatus(player);
  }
  
  @Override
  public void pollIngameStatus(OsuUser player, PollStatusConsumer action) {
    bot.pollIngameStatus(player, action);
  }
  
  @Override
  public void restartClient() throws IOException, InterruptedException {
    app.killOsu();
    
    Thread.sleep(5000);
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      QueuedPlayer currentPlayer = spectator.getCurrentPlayer(pm);
      if (currentPlayer != null) {
        startSpectate(currentPlayer.getPlayer());
      }
    } finally {
      pm.close();
    }
  }
}
