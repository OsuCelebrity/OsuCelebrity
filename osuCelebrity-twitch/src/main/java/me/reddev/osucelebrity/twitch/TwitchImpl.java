package me.reddev.osucelebrity.twitch;

import static me.reddev.osucelebrity.twitch.QTwitchUser.twitchUser;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.core.SkipReason;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;
import me.reddev.osucelebrity.twitchapi.TwitchApiUser;

import java.io.IOException;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchImpl implements Twitch {
  final OsuApi osuApi;
  final TwitchIrcSettings ircSettings;
  final TwitchApiSettings apiSettings;
  final PersistenceManagerFactory pmf;
  final TwitchIrcBot bot;
  final TwitchApi api;

  @Override
  public void whisperUser(String nick, String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void announcePlayerSkipped(SkipReason reason, OsuUser player) {
    if (reason == SkipReason.OFFLINE) {
      bot.sendMessage(String.format(TwitchResponses.SKIPPED_OFFLINE, player.getUserName()));
    }
    if (reason == SkipReason.IDLE) {
      bot.sendMessage(String.format(TwitchResponses.SKIPPED_IDLE, player.getUserName()));
    }
    if (reason == SkipReason.BANNED_MAP) {
      bot.sendMessage(String.format(TwitchResponses.BANNED_MAP, player.getUserName()));
    }
  }

  @Override
  public TwitchUser getUser(PersistenceManager pm, String username, long maxAge) throws IOException {
    TwitchApiUser apiUser = api.getUser(pm, username, maxAge);

    try (JDOQuery<TwitchUser> query =
        new JDOQuery<TwitchUser>(pm).select(twitchUser).from(twitchUser)
            .where(twitchUser.user.eq(apiUser))) {

      TwitchUser user = query.fetchOne();
      if (user != null) {
        return user;
      }

      return pm.makePersistent(new TwitchUser(apiUser));
    }
  }
}
