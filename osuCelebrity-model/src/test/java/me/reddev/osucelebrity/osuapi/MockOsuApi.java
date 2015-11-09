package me.reddev.osucelebrity.osuapi;

import static me.reddev.osucelebrity.osu.QOsuIrcUser.osuIrcUser;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;

import com.querydsl.jdo.JDOQuery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osu.OsuUser;

import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;

import javax.jdo.PersistenceManager;

public class MockOsuApi implements OsuApi {
  @Override
  public OsuUser getUser(int userid, int gameMode, PersistenceManager pm, long maxAge)
      throws IOException {
    JDOQuery<OsuUser> query =
        new JDOQuery<>(pm).select(osuUser).from(osuUser).where(osuUser.userId.eq(userid));
    OsuUser user = query.fetchOne();
    if (user == null) {
      throw new IllegalStateException("Unexpected: user id " + userid + " was not created.");
    }
    return user;
  }

  int userid = 0;

  @Override
  @SuppressFBWarnings("TQ")
  public synchronized OsuUser getUser(String ircUserName, int gameMode, PersistenceManager pm,
      long maxAge) {
    JDOQuery<OsuUser> query =
        new JDOQuery<>(pm).select(osuUser).from(osuUser).where(osuUser.userName.eq(ircUserName));

    OsuUser user = query.fetchOne();

    if (user != null) {
      return user;
    }

    OsuApiUser apiUser = new OsuApiUser();
    apiUser.setUserId(userid++);
    apiUser.setUserName(ircUserName);
    apiUser.setMode(gameMode);
    apiUser.setRank(1000);
    apiUser.setPp(1000);
    user = new OsuUser(apiUser, System.currentTimeMillis());
    return pm.makePersistent(user);
  }

  @Override
  public OsuIrcUser getIrcUser(String ircUserName, int gameMode, PersistenceManager pm, long maxAge) {
    JDOQuery<OsuIrcUser> query =
        new JDOQuery<>(pm).select(osuIrcUser).from(osuIrcUser)
            .where(osuIrcUser.ircName.eq(ircUserName));

    OsuIrcUser ircUser = query.fetchOne();

    if (ircUser == null) {
      ircUser =
          new OsuIrcUser(ircUserName, getUser(ircUserName, gameMode, pm, maxAge),
              System.currentTimeMillis());
      pm.makePersistent(ircUser);
    }

    return ircUser;
  }
}
