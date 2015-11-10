package me.reddev.osucelebrity.twitch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.osuapi.OsuApi;

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
  public void sendMessageToChannel(String message) {
    bot.sendMessage(message);
  }

  @Override
  public void whisperUser(String nick, String message) {
    // TODO Auto-generated method stub

  }
}
