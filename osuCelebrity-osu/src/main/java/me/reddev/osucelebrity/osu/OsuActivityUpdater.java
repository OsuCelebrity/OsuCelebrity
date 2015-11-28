package me.reddev.osucelebrity.osu;

import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;
import static me.reddev.osucelebrity.osuapi.QApiUser.apiUser;

import com.querydsl.jdo.JDOQuery;
import com.querydsl.sql.SQLExpressions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.osuapi.ApiUser;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiScore;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuActivityUpdater {
  private final Downloader downloader;

  private final PersistenceManagerFactory pmf;

  private final Clock clock;

  /**
   * Update activity for one player.
   */
  public void update() {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      createMissingActivity(pm);
      updateActivity(pm);
    } catch (SocketTimeoutException e) {
      // yeah, whatever
    } catch (IOException e) {
      log.warn("exception", e);
    } catch (Exception e) {
      log.error("exception", e);
    } finally {
      pm.close();
    }
  }

  void updateActivity(PersistenceManager pm) throws IOException {
    try (JDOQuery<PlayerActivity> userQuery =
        new JDOQuery<PlayerActivity>(pm).select(playerActivity).from(playerActivity)
            .where(playerActivity.user.rank.loe(1000)).orderBy(playerActivity.lastChecked.asc())) {
      PlayerActivity first = userQuery.fetchFirst();
      if (first == null) {
        return;
      }
      List<OsuApiScore> userRecent =
          downloader.getUserRecent(first.getUser().getUserId(), first.getUser().getGameMode(),
              OsuApiScore.class);
      first.update(userRecent, clock.getTime());
    }
  }

  void createMissingActivity(PersistenceManager pm) {
    try (JDOQuery<ApiUser> userQuery =
        new JDOQuery<PlayerActivity>(pm)
            .select(apiUser)
            .from(apiUser)
            .where(
                apiUser.gameMode.eq(GameModes.OSU),
                SQLExpressions.select(playerActivity).from(playerActivity)
                    .where(playerActivity.user.eq(apiUser)).exists().not())) {
      List<ApiUser> newUsers = userQuery.fetch();
      newUsers.stream().map(user -> new PlayerActivity(user, 0L, 0L)).forEach(pm::makePersistent);
    }
  }
}
