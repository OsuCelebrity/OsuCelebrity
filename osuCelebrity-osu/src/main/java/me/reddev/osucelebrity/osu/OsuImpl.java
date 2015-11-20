package me.reddev.osucelebrity.osu;

import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.jdo.PersistenceManager;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuImpl implements Osu {
  private final OsuIrcBot bot;
  private final OsuApplication app;

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
  public void notifyStarting(OsuUser user) {
    // TODO enable after alpha
    // bot.notifyStartingPlayer(user);
  }

  @Override
  public void notifySoon(OsuUser player) {
    // TODO enable after alpha
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
}
