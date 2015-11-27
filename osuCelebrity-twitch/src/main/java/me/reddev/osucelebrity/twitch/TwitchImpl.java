package me.reddev.osucelebrity.twitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.core.SkipReason;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;

import javax.inject.Inject;
import javax.jdo.PersistenceManagerFactory;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchImpl implements Twitch {
  final OsuApi osuApi;
  final TwitchIrcSettings ircSettings;
  final TwitchApiSettings apiSettings;
  final PersistenceManagerFactory pmf;
  final TwitchIrcBot bot;

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
  }
}
