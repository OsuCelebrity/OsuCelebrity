package me.reddev.osucelebrity;

import static me.reddev.osucelebrity.osu.QOsuIrcUser.osuIrcUser;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;
import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;
import static me.reddev.osucelebrity.osuapi.QApiUser.apiUser;

import com.querydsl.jdo.JDOQuery;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osu.PlayerActivity;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiScore;
import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuApiImpl implements OsuApi {
  private final Downloader downloader;
  private final Clock clock;

  @Override
  public OsuUser getUser(int userid, PersistenceManager pm, long maxAge) throws IOException {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      final OsuUser saved = query.where(osuUser.userId.eq(userid)).fetchOne();

      if (saved != null && (maxAge <= 0 || saved.getDownloaded() >= clock.getTime() - maxAge)) {
        return saved;
      }
      try {
        OsuApiUser apiUser = downloader.getUser(userid, GameModes.OSU, OsuApiUser.class);
        if (apiUser == null) {
          return null;
        }
        saveModSpecific(pm, apiUser);
        if (saved == null) {
          return pm.makePersistent(new OsuUser(apiUser, clock.getTime()));
        }
        saved.update(apiUser, clock.getTime());
        return saved;
      } catch (SocketTimeoutException e) {
        if (saved != null) {
          return saved;
        }
        throw e;
      }
    }
  }

  @SuppressFBWarnings("TQ")
  @Override
  public OsuUser getUser(String userName, PersistenceManager pm, long maxAge) throws IOException {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      final OsuUser saved =
          query.where(osuUser.userName.lower().eq(userName.toLowerCase())).fetchOne();

      if (saved != null && (maxAge <= 0 || saved.getDownloaded() >= clock.getTime() - maxAge)) {
        return saved;
      }
      try {
        OsuApiUser apiUser = downloader.getUser(userName, GameModes.OSU, OsuApiUser.class);
        if (apiUser == null) {
          return null;
        }
        saveModSpecific(pm, apiUser);
        if (saved == null) {
          if (!userName.equalsIgnoreCase(apiUser.getUserName())) {
            // this might have come from irc with let's make sure to save
            try (JDOQuery<OsuUser> queryRetry = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
              final OsuUser savedRetry =
                  query.where(osuUser.userName.lower().eq(apiUser.getUserName().toLowerCase()))
                      .fetchOne();
              if (savedRetry != null) {
                savedRetry.update(apiUser, clock.getTime());
                return savedRetry;
              }
            }
          }
          return pm.makePersistent(new OsuUser(apiUser, clock.getTime()));
        }
        saved.update(apiUser, clock.getTime());
        return saved;
      } catch (SocketTimeoutException e) {
        if (saved != null) {
          return saved;
        }
        throw e;
      }
    }
  }

  void saveModSpecific(PersistenceManager pm, OsuApiUser downloaded) {
    try (JDOQuery<ApiUser> query = new JDOQuery<>(pm).select(apiUser).from(apiUser)) {
      ApiUser saved =
          query.where(apiUser.userId.eq(downloaded.getUserId()),
              apiUser.gameMode.eq(downloaded.getMode())).fetchOne();

      if (saved == null) {
        pm.makePersistent(new ApiUser(downloaded, clock.getTime()));
      } else {
        saved.update(downloaded, clock.getTime());
      }
    }
  }

  void saveGeneral(PersistenceManager pm, OsuApiUser downloaded) {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      OsuUser saved = query.where(osuUser.userId.eq(downloaded.getUserId())).fetchOne();

      if (saved == null) {
        pm.makePersistent(new OsuUser(downloaded, clock.getTime()));
      } else {
        saved.update(downloaded, clock.getTime());
      }
    }
  }

  @SuppressFBWarnings("TQ")
  @Override
  public OsuIrcUser getIrcUser(String ircUserName, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuIrcUser> query = new JDOQuery<>(pm).select(osuIrcUser).from(osuIrcUser)) {
      OsuIrcUser saved =
          query.where(osuIrcUser.ircName.lower().eq(ircUserName.toLowerCase())).fetchOne();
      if (saved == null) {
        OsuUser user = getUser(ircUserName, pm, maxAge);
        saved = new OsuIrcUser(ircUserName, user, clock.getTime());
        return pm.makePersistent(saved);
      }
      if (maxAge <= 0 || saved.getResolved() >= clock.getTime() - maxAge) {
        return saved;
      }
      OsuUser user;
      try {
        user = getUser(ircUserName, pm, maxAge);
      } catch (SocketTimeoutException e) {
        return saved;
      }
      saved.setUser(user);
      if (user == null) {
        saved.setResolved(clock.getTime());
      } else {
        saved.setResolved(user.getDownloaded());
      }
      return saved;
    }
  }

  @Override
  public ApiUser getUserData(int userid, int gameMode, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<ApiUser> query = new JDOQuery<>(pm).select(apiUser).from(apiUser)) {
      ApiUser saved =
          query.where(apiUser.userId.eq(userid), apiUser.gameMode.eq(gameMode)).fetchOne();

      if (saved != null && (maxAge <= 0 || saved.getDownloaded() >= clock.getTime() - maxAge)) {
        return saved;
      }
      try {
        OsuApiUser apiUser = downloader.getUser(userid, GameModes.OSU, OsuApiUser.class);
        if (apiUser == null) {
          return null;
        }
        saveGeneral(pm, apiUser);
        if (saved == null) {
          return pm.makePersistent(new ApiUser(apiUser, clock.getTime()));
        }
        saved.update(apiUser, clock.getTime());
        return saved;
      } catch (SocketTimeoutException e) {
        if (saved != null) {
          return saved;
        }
        throw e;
      }
    }
  }
  
  @Override
  public PlayerActivity getPlayerActivity(ApiUser user, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<PlayerActivity> userQuery =
        new JDOQuery<PlayerActivity>(pm).select(playerActivity).from(playerActivity)
            .where(playerActivity.user.eq(user))) {
      PlayerActivity activity = userQuery.fetchFirst();
      if (activity == null) {
        activity = pm.makePersistent(new PlayerActivity(user, 0, 0));
      } else if (maxAge <= 0 || activity.getLastChecked() >= clock.getTime() - maxAge) {
        return activity;
      }
      List<OsuApiScore> userRecent =
          downloader.getUserRecent(activity.getUser().getUserId(),
              activity.getUser().getGameMode(), OsuApiScore.class);
      activity.update(userRecent, clock.getTime());
      return activity;
    }
  }
}
