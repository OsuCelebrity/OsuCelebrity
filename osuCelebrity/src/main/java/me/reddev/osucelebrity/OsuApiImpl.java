package me.reddev.osucelebrity;

import static me.reddev.osucelebrity.osu.QOsuIrcUser.osuIrcUser;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;

import com.querydsl.jdo.JDOQuery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;

import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class OsuApiImpl implements OsuApi {
  private final Downloader downloader;
  private final Clock clock;
  
  @Override
  public OsuUser getUser(int userid, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      OsuUser user = query.where(osuUser.userId.eq(userid)).fetchOne();

      if (user == null) {
        log.debug("downloading user " + userid);
        OsuApiUser apiUser = downloader.getUser(userid, GameModes.OSU, OsuApiUser.class);
        if (apiUser == null) {
          return null;
        }

        return pm.makePersistent(new OsuUser(apiUser, clock.getTime()));
      } else if (user.getDownloaded() < clock.getTime() - maxAge) {
        try {
          log.debug("downloading user " + userid);
          OsuApiUser apiUser = downloader.getUser(userid, GameModes.OSU, OsuApiUser.class);
          user.update(apiUser, clock.getTime());
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
  public OsuUser getUser(String userName, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuUser> query = new JDOQuery<>(pm).select(osuUser).from(osuUser)) {
      OsuUser user = query.where(osuUser.userName.eq(userName)).fetchOne();

      if (user != null) {
        // we'll query again here, but meh
        return getUser(user.getUserId(), pm, maxAge);
      }
      log.debug("downloading user " + userName);
      OsuApiUser apiUser = downloader.getUser(userName, GameModes.OSU, OsuApiUser.class);
      if (apiUser == null) {
        return null;
      }
      return getUser(apiUser.getUserId(), pm, maxAge);
    }
  }

  @SuppressFBWarnings("TQ")
  @Override
  public OsuIrcUser getIrcUser(String ircUserName, PersistenceManager pm, long maxAge)
      throws IOException {
    try (JDOQuery<OsuIrcUser> query = new JDOQuery<>(pm).select(osuIrcUser).from(osuIrcUser)) {
      OsuIrcUser ircUser = query.where(osuIrcUser.ircName.eq(ircUserName)).fetchOne();
      if (ircUser == null) {
        OsuUser user = getUser(ircUserName, pm, maxAge);
        ircUser = new OsuIrcUser(ircUserName, user, clock.getTime());
        return pm.makePersistent(ircUser);
      }
      if (ircUser.getResolved() < clock.getTime() - maxAge) {
        OsuUser user;
        try {
          user = getUser(ircUserName, pm, maxAge);
        } catch (SocketTimeoutException e) {
          return ircUser;
        }
        ircUser.setUser(user);
        if (user == null) {
          ircUser.setResolved(clock.getTime());
        } else {
          ircUser.setResolved(user.getDownloaded());
        }
        return ircUser;
      }
      return ircUser;
    }
  }
}
