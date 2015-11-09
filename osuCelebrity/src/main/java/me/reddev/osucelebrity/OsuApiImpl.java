package me.reddev.osucelebrity;

import static me.reddev.osucelebrity.osu.QOsuIrcUser.osuIrcUser;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;

import com.querydsl.jdo.JDOQuery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;

import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.jdo.PersistenceManager;

@Slf4j
@AllArgsConstructor
public class OsuApiImpl implements OsuApi {
  Downloader downloader;

  @Override
  public OsuUser getUser(int userid, int gameMode, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      OsuUser user = query.where(osuUser.userId.eq(userid)).fetchOne();

      if (user == null) {
        log.debug("downloading user " + userid);
        OsuApiUser apiUser = downloader.getUser(userid, gameMode, OsuApiUser.class);
        if (apiUser == null) {
          return null;
        }

        user = new OsuUser(apiUser, System.currentTimeMillis());
        return user;
      } else if (user.getDownloaded() < System.currentTimeMillis() - maxAge) {
        try {
          log.debug("downloading user " + userid);
          OsuApiUser apiUser = downloader.getUser(userid, gameMode, OsuApiUser.class);
          user.update(apiUser);
          user.setDownloaded(System.currentTimeMillis());
          return user;
        } catch (SocketTimeoutException e) {
          return user;
        }
      }

      return user;
    }
  }

  @SuppressFBWarnings("TQ")
  @Override
  public OsuUser getUser(String userName, int gameMode, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      OsuUser user = query.where(osuUser.userName.eq(userName)).fetchOne();

      if (user != null) {
        // we'll query again here, but meh
        return getUser(user.getUserId(), gameMode, pm, maxAge);
      }
      log.debug("downloading user " + userName);
      OsuApiUser apiUser = downloader.getUser(userName, gameMode, OsuApiUser.class);
      if (apiUser == null) {
        return null;
      }
      return getUser(apiUser.getUserId(), gameMode, pm, maxAge);
    }
  }

  @SuppressFBWarnings("TQ")
  @Override
  public OsuIrcUser getIrcUser(String ircUserName, int gameMode, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuIrcUser> query = new JDOQuery<>(pm).select(osuIrcUser).from(osuIrcUser)) {
      OsuIrcUser ircUser = query.where(osuIrcUser.ircName.eq(ircUserName)).fetchOne();
      if (ircUser == null) {
        OsuUser user = getUser(ircUserName, gameMode, pm, maxAge);
        ircUser = new OsuIrcUser(ircUserName, user, System.currentTimeMillis());
        return pm.makePersistent(ircUser);
      }
      if (ircUser.getResolved() < System.currentTimeMillis() - maxAge) {
        OsuUser user;
        try {
          user = getUser(ircUserName, gameMode, pm, maxAge);
        } catch (SocketTimeoutException e) {
          return ircUser;
        }
        ircUser.setUser(user);
        if (user == null) {
          ircUser.setResolved(System.currentTimeMillis());
        } else {
          ircUser.setResolved(user.getDownloaded());
        }
        return ircUser;
      }
      return ircUser;
    }
  }
}
