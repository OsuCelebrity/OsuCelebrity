package me.reddev.osucelebrity.twitch;

import lombok.Data;

@Data
public abstract class AbstractTwitchCommand implements TwitchCommand {
  final Twitch twitch;
  final String user;
}
