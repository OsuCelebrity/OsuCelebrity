package me.reddev.osucelebrity.twitch;

import static me.reddev.osucelebrity.twitchapi.QTwitchApiUser.twitchApiUser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import com.querydsl.jdo.JDOQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.reddev.osucelebrity.core.Clock;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.StatusWindow;
import me.reddev.osucelebrity.twitch.TwitchApiImpl.Broadcasts.Video;
import me.reddev.osucelebrity.twitchapi.TwitchApi;
import me.reddev.osucelebrity.twitchapi.TwitchApiSettings;
import me.reddev.osucelebrity.twitchapi.TwitchApiUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.jdo.PersistenceManager;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TwitchApiImpl implements TwitchApi {
  @Mapper
  public interface FromApiMapper {
    @Mappings({@Mapping(target = "id", ignore = true)})
    void update(TwitchApiUser user, @MappingTarget TwitchApiUser target);
  }

  private static final String TMI_BASE = "http://tmi.twitch.tv/";
  private static final String CHATDEPOT_BASE = "http://chatdepot.twitch.tv/";

  private static final String CHATTERS = "group/user/%s/chatters?_=%d";
  
  private static final String USER = "https://api.twitch.tv/kraken/users/%s";
  
  private static final String BROADCASTS = "https://api.twitch.tv/kraken/channels/%s/videos?broadcasts=true";

  private final TwitchApiSettings settings;
  private final TwitchIrcSettings ircSettings;
  private final Clock clock;
  private final StatusWindow statusWindow;

  private String readString(InputStream is) throws IOException, UnsupportedEncodingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    for (int len = 0; (len = is.read(buf)) > 0;) {
      baos.write(buf, 0, len);
    }
    return baos.toString("UTF-8");
  }

  private String readString(URL url) throws IOException, UnsupportedEncodingException {
    URLConnection conn = url.openConnection();

    return readString(conn.getInputStream());
  }

  private String getTmiRequest(String uri) throws IOException {
    URL url = new URL(TMI_BASE + uri);
    return readString(url);
  }

  private String getRoomMembershipsRequest() throws IOException {
    URL url =
        new URL(CHATDEPOT_BASE + "room_memberships?oauth_token="
            + settings.getTwitchToken().substring("oauth:".length()));
    return readString(url);
  }

  @CheckForNull
  ChannelChatters channelChatters = null;

  /**
   * updates channel chatters through chatters API. exceptions are logged.
   */
  public void updateChatters() {
    try {
      String req =
          getTmiRequest(String.format(CHATTERS, ircSettings.getTwitchIrcUsername().toLowerCase(),
              clock.getTime()));

      channelChatters = new Gson().fromJson(req, ChannelChatters.class);
      statusWindow.setTwitchMods(channelChatters.getChatters().getModerators());
    } catch (IOException e) {
      // we are expecting these regularly, so no need to put them in the error log
      log.warn("Exception while updating chatters", e);
    } catch (Exception e) {
      log.error("Exception while updating chatters", e);
    }
  }

  @Override
  public boolean isModerator(String username) {
    List<String> onlineMods = getOnlineMods();
    return onlineMods.contains(username.toLowerCase());
  }

  @Data
  public class ChannelChatters {
    int chatterCount;
    Chatters chatters;

    @Data
    public class Chatters {
      List<String> moderators;
      List<String> staff;
      List<String> admins;
      List<String> globalMods;
      List<String> viewers;
    }
  }

  @Data
  public static class RoomMemberships {
    List<Membership> memberships;

    @Data
    public static class Membership {
      Room room;

      @Data
      public static class Room {
        @SerializedName("irc_channel")
        String ircChannel;
        List<String> servers;
      }
    }
  }
  
  @Data
  public static class Broadcasts {
    List<Video> videos;
    
    @Data
    public static class Video {
      @SerializedName("_id")
      String id;
      @SerializedName("broadcast_id")
      long broadcastId;
      String title;
      double length;
      String url;
      String status;
      @SerializedName("recorded_at")
      Date recordedAt;
      @SerializedName("created_at")
      Date createdAt;
      @SerializedName("delete_at")
      Date deleteAt;
    }
  }

  @Override
  public List<String> getOnlineMods() {
    return channelChatters != null ? channelChatters.getChatters().getModerators() : Collections
        .emptyList();
  }

  @Override
  public List<Entry<String, List<String>>> getRoomMemberships() throws IOException {
    String req = getRoomMembershipsRequest();

    RoomMemberships memberships = new Gson().fromJson(req, RoomMemberships.class);

    return memberships.memberships.stream().map(mship -> mship.room)
        .map(room -> Pair.of(room.ircChannel, room.servers)).collect(Collectors.toList());
  }
  
  Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();

  @Override
  public TwitchApiUser getUser(PersistenceManager pm, String username, long maxAge)
      throws IOException {
    try (JDOQuery<TwitchApiUser> query =
        new JDOQuery<>(pm).select(twitchApiUser).from(twitchApiUser)
            .where(twitchApiUser.name.eq(username))) {
      TwitchApiUser cached = query.fetchOne();
      
      if (cached == null) {
        return pm.makePersistent(downloadUser(username));
      }
      
      if (maxAge <= 0 || cached.getDownloaded() > clock.getTime() - maxAge) {
        return cached;
      }
      
      new FromApiMapperImpl().update(downloadUser(username), cached);
      return cached;
    }
  }

  private TwitchApiUser downloadUser(String username) throws IOException {
    TwitchApiUser user =
        gson.fromJson(readString(new URL(String.format(USER, username))), TwitchApiUser.class);
    user.setDownloaded(clock.getTime());
    return user;
  }
  
  List<Video> getBroadcasts() throws IOException {
    Broadcasts broadcasts =
        gson.fromJson(
            readString(new URL(String.format(BROADCASTS, ircSettings.getTwitchIrcUsername()
                .toLowerCase()))), Broadcasts.class);

    return broadcasts.getVideos();
  }
  
  @Override
  public URL getReplayLink(QueuedPlayer play) throws IOException {
    Optional<Video> match = getBroadcasts()
        .stream()
        .filter(video -> video.getRecordedAt().getTime() < play.getStartedAt())
        .filter(
            video -> video.getRecordedAt().getTime() + video.getLength() * 1000 > play
                .getStartedAt() || video.getStatus().equals("recording")).findFirst();
    
    if (!match.isPresent()) {
      return null;
    }
    
    String offset = getOffset(play.getStartedAt() - match.get().getRecordedAt().getTime());
    return new URL(match.get().getUrl() + "?t=" + offset);
  }
  
  String getOffset(long timeInMillis) {
    timeInMillis /= 1000;
    String offset = StringUtils.leftPad(String.valueOf(timeInMillis % 60), 2, '0') + "s";

    timeInMillis /= 60;
    if (timeInMillis <= 0) {
      return offset;
    }
    offset = StringUtils.leftPad(String.valueOf(timeInMillis % 60), 2, '0') + "m" + offset;

    timeInMillis /= 60;
    if (timeInMillis <= 0) {
      return offset;
    }
    offset = StringUtils.leftPad(String.valueOf(timeInMillis % 24), 2, '0') + "h" + offset;

    return offset;
  }
}
