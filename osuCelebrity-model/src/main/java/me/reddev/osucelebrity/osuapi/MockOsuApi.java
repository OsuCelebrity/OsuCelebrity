package me.reddev.osucelebrity.osuapi;

import org.tillerino.osuApiModel.OsuApiUser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockOsuApi implements OsuApi {
  Map<String, Integer> ids = new HashMap<>();
  Map<Integer, String> names = new ConcurrentHashMap<>();

  @Override
  public OsuApiUser getUser(int userid, int gameMode, long maxAge) throws IOException,
  SQLException {
    if (!names.containsKey(userid)) {
      return null;
    }
    final OsuApiUser user = new OsuApiUser();
    user.setUserName(names.get(userid));
    user.setUserId(userid);
    user.setLevel(gameMode);
    user.setRank(1000);
    user.setPp(1000);
    return user;
  }

  @Override
  public synchronized OsuApiUser getUser(String ircUserName, int gameMode, long maxAge)
      throws IOException, SQLException {
    if (!ids.containsKey(ircUserName)) {
      final int newId = ids.size();
      ids.put(ircUserName, newId);
      names.put(newId, ircUserName);
    }
    return getUser(ids.get(ircUserName), gameMode, maxAge);
  }
}
