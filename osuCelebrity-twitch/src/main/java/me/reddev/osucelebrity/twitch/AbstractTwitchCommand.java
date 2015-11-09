package me.reddev.osucelebrity.twitch;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.jdo.PersistenceManager;

@Data
@EqualsAndHashCode(exclude = {"twitch", "persistenceManager"})
public abstract class AbstractTwitchCommand implements TwitchCommand {
  final Twitch twitch;
  final String user;
  final PersistenceManager persistenceManager;
}
