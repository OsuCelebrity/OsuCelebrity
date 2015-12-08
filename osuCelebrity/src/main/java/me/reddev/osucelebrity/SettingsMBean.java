package me.reddev.osucelebrity;

import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.osu.OsuApplication.OsuApplicationSettings;
import me.reddev.osucelebrity.osu.OsuIrcSettings;
import me.reddev.osucelebrity.osuapi.OsuApiSettings;
import me.reddev.osucelebrity.twitch.TwitchIrcSettings;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;

//CHECKSTYLE:OFF
public interface SettingsMBean extends OsuIrcSettings, TwitchIrcSettings, TwitchApiSettings,
    OsuApplicationSettings, OsuApiSettings, CoreSettings {

  public void reload();
}
