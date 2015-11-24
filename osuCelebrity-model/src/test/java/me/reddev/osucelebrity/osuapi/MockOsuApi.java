package me.reddev.osucelebrity.osuapi;

import me.reddev.osucelebrity.osu.PlayerActivity;

import static me.reddev.osucelebrity.osu.QOsuIrcUser.osuIrcUser;
import static me.reddev.osucelebrity.osu.QOsuUser.osuUser;
import static me.reddev.osucelebrity.osuapi.QApiUser.apiUser;
import static me.reddev.osucelebrity.osu.QPlayerActivity.playerActivity;

import com.querydsl.jdo.JDOQuery;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.reddev.osucelebrity.osu.OsuIrcUser;
import me.reddev.osucelebrity.osu.OsuUser;
import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;

import javax.jdo.PersistenceManager;

public class MockOsuApi implements OsuApi {
  @Override
  public OsuUser getUser(int userid, PersistenceManager pm, long maxAge) throws IOException {
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
  public synchronized OsuUser getUser(String ircUserName, PersistenceManager pm, long maxAge) {
    JDOQuery<OsuUser> query =
        new JDOQuery<>(pm).select(osuUser).from(osuUser).where(osuUser.userName.eq(ircUserName));

    OsuUser user = query.fetchOne();

    if (user != null) {
      return user;
    }

    OsuApiUser apiUser = new OsuApiUser();
    apiUser.setUserId(userid++);
    apiUser.setUserName(ircUserName);
    user = new OsuUser(apiUser, System.currentTimeMillis());
    return pm.makePersistent(user);
  }

  @Override
  public OsuIrcUser getIrcUser(String ircUserName, PersistenceManager pm, long maxAge) {
    JDOQuery<OsuIrcUser> query =
        new JDOQuery<>(pm).select(osuIrcUser).from(osuIrcUser)
            .where(osuIrcUser.ircName.eq(ircUserName));

    OsuIrcUser ircUser = query.fetchOne();

    if (ircUser == null) {
      ircUser =
          new OsuIrcUser(ircUserName, getUser(ircUserName, pm, maxAge), System.currentTimeMillis());
      pm.makePersistent(ircUser);
    }

    return ircUser;
  }

  @Override
  public ApiUser getUserData(int userid, int gameMode, PersistenceManager pm, long maxAge) {
    JDOQuery<ApiUser> query =
        new JDOQuery<>(pm).select(apiUser).from(apiUser).where(apiUser.userId.eq(userid));

    ApiUser savedUser = query.fetchOne();
    if (savedUser != null) {
      return savedUser;
    }

    ApiUser apiUser = new ApiUser(userid, gameMode);
    apiUser.setPlayCount(1000);
    apiUser.setDownloaded(System.currentTimeMillis());
    return pm.makePersistent(apiUser);
  }

  @Override
  public PlayerActivity getPlayerActivity(ApiUser user, PersistenceManager pm, long maxAge)
      throws IOException {
    PlayerActivity activity = new JDOQuery<>(pm).select(playerActivity).from(playerActivity).where(playerActivity.user.eq(user)).fetchOne();
    
    if(activity == null) {
      activity = pm.makePersistent(new PlayerActivity(user, 0, 0));
    }
    
    return activity;
  }
}
