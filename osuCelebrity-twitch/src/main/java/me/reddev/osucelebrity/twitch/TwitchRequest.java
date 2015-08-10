package me.reddev.osucelebrity.twitch;

import java.util.LinkedList;
import java.util.Queue;

import org.tillerino.osuApiModel.OsuApiUser;

public class TwitchRequest {
  private Queue<OsuApiUser> _requestedUsers = new LinkedList<OsuApiUser>();

  /**
   * Adds a new user to the request queue. If exists, adds to the end
   * 
   * @param user The user object to request
   */
  public void AddRequest(OsuApiUser user) {
    if (_requestedUsers.contains(user))
      _requestedUsers.remove(user);
    _requestedUsers.add(user);
  }

  /**
   * Deletes all pending requests
   */
  public void clearRequests() {
    _requestedUsers.clear();
  }

  /**
   * @return The amount of requested users in the queue
   */
  public int getRequestCount() {
    return _requestedUsers.size();
  }

  public Queue<OsuApiUser> getRequestedUsers() {
    return _requestedUsers;
  }
}
