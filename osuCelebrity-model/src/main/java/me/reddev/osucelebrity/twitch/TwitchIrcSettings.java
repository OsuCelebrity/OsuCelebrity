package me.reddev.osucelebrity.twitch;

public interface TwitchIrcSettings {
  String getTwitchIrcHost();

  int getTwitchIrcPort();

  String getTwitchIrcChannel();

  String getTwitchIrcUsername();

  String getTwitchToken();

  String getTwitchIrcCommand();
}
