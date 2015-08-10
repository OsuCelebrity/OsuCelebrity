package me.reddev.osucelebrity.twitch;

import org.tillerino.osuApiModel.OsuApiUser;

import java.util.LinkedList;
import java.util.Queue;

public class TwitchRequest {
  private Queue<OsuApiUser> requestedUsers = new LinkedList<OsuApiUser>();

  /**
   * Adds a new user to the request queue. If exists, adds to the end
   * 
   * @param user The user object to request
   */
  public void addRequest(OsuApiUser user) {
    if (requestedUsers.contains(user)) {
      requestedUsers.remove(user);
    }
    requestedUsers.add(user);
  }

  /**
   * Deletes all pending requests.
   */
  public void clearRequests() {
    requestedUsers.clear();
  }

  /**
   * @return The amount of requested users in the queue.
   */
  public int getRequestCount() {
    return requestedUsers.size();
  }

  public Queue<OsuApiUser> getRequestedUsers() {
    return requestedUsers;
  }
}
