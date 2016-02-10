package me.reddev.osucelebrity.core;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.TwitchResponses;
import me.reddev.osucelebrity.UserException;
import me.reddev.osucelebrity.twitch.TwitchUser;

import java.io.IOException;
import java.sql.Date;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TrustImpl implements Trust {
  final Clock clock;

  final CoreSettings settings;

  @Override
  public void checkTrust(PersistenceManager pm, TwitchUser user) throws IOException, UserException {
    if (!user.isTrusted()) {
      if (user.getOsuUser() != null) {
        // as soon as an account is linked, we trust it.
        return;
      }
      throw new UserException(TwitchResponses.UNTRUSTED);
    }

    Date trustDate = new Date(clock.getTime() - settings.getTwitchTrustAccountAge());

    if (user.getUser().getCreatedAt().compareTo(trustDate) > 0) {
      user.setTrusted(false);
      throw new UserException(TwitchResponses.UNTRUSTED);
    }
  }
}
