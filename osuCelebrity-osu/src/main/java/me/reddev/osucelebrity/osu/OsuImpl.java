package me.reddev.osucelebrity.osu;

import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;

import com.github.omkelderman.osudbparser.OsuBeatmapInfo;
import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.OsuResponses;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
  public void refreshSpectate(OsuUser user) {
    try {
      app.refreshSpec(user);
    } catch (IOException e) {
      log.error("error spectating", e);
    }
  }

  @Override
  public void message(OsuUser user, String message) {
    bot.messagePlayer(user, message);
  }

  @Override
  public void notifyStarting(OsuUser player) {
    message(player, String.format(OsuResponses.SPECTATING_NOW));
  }
  
  @Override
  public void notifyNext(OsuUser player) {
    message(player, String.format(OsuResponses.SPECTATING_NEXT));
  }
  
  @Override
  public void notifyDone(OsuUser player) {
    message(player, String.format(OsuResponses.DONE_SPECTATING));
  }
  
  @Override
  public void notifyQueued(OsuUser player, int queuePosition) {
    message(player, String.format(OsuResponses.QUEUED, queuePosition));
  }
  
  @Override
  public void notifyStatistics(OsuUser player, long danks, long skips) {
    message(player, String.format(OsuResponses.STATISTICS, danks, skips));
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
    respecCurrentPlayer();
    /*
     * wait and send the command a second time. it seems like the osu client ignores the first time.
     */
    Thread.sleep(5000);
    respecCurrentPlayer();
  }

  private void respecCurrentPlayer() {
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

  @Override
  public Integer getBeatmapId(String formattedName) {
    Collection<OsuBeatmapInfo> beatmaps = app.beatmaps.get(formattedName);
    if (beatmaps.size() != 1) {
      return null;
    }
    return (int) beatmaps.stream().findFirst().get().getBeatmapId();
  }
}
