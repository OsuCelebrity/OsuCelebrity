package me.reddev.osucelebrity.core.api;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.QueuedPlayer.QueueSource;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osu.OsuUser;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osuapi.OsuApi;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.osuApiModel.types.GameMode;
import org.tillerino.osuApiModel.types.UserId;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Singleton
@Path("/current")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class CurrentPlayerService {
  @Data
  public static class CurrentPlayer {
    String name;

    @UserId
    @Setter(onParam = @__(@UserId))
    @Getter(onMethod = @__(@UserId))
    int id;

    String playingFor;

    double health;

    String beatmap;

    int rank;

    double pp;

    int playCount;

    double level;

    double accuracy;

    String country;
    
    int gameMode;

    String nextPlayer;
    
    String source;
    
    int queueSize;
  }

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;

  private final CoreSettings coreSettings;

  private final Osu osu;

  private final OsuApi api;

  private final ExecutorService exec;

  /**
   * Shows details about the player currently being spectated.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public CurrentPlayer current() {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      CurrentPlayer response = new CurrentPlayer();
      QueuedPlayer queued = spectator.getCurrentPlayer(pm);
      if (queued != null) {
        OsuUser player = queued.getPlayer();
        response.setName(player.getUserName());

        response.setPlayingFor(formatDuration(System.currentTimeMillis() - queued.getStartedAt()));

        long timeLeft = Math.max(0, queued.getStoppingAt() - queued.getLastRemainingTimeUpdate());
        response.setHealth(timeLeft / (double) coreSettings.getDefaultSpecDuration());
        response.setId(player.getUserId());
        response.setSource(queued.getQueueSource() == QueueSource.AUTO ? "auto" : "queue");

        ApiUser apiUser = getApiUser(response, queued.getPlayer());

        if (apiUser != null) {
          response.rank = apiUser.getRank();
          response.pp = apiUser.getPp();
          response.playCount = apiUser.getPlayCount();
          response.level = apiUser.getLevel();
          response.accuracy = apiUser.getAccuracy();
          response.country = apiUser.getCountry();
          response.gameMode = apiUser.getGameMode();
        }
      }
      OsuStatus clientStatus = osu.getClientStatus();
      if (clientStatus.getType() == Type.PLAYING) {
        response.setBeatmap(clientStatus.getDetail());
      }
      QueuedPlayer nextPlayer = spectator.getNextPlayer(pm);
      if (nextPlayer != null) {
        response.nextPlayer = nextPlayer.getPlayer().getUserName();
      }
      response.setQueueSize(spectator.getQueueSize(pm));
      return response;
    } finally {
      pm.close();
    }
  }

  /**
   * Format duration as a human-readable time string.
   * @param millis duration in milliseconds.
   * @return hh:mm:ss or mm:ss
   */
  public static String formatDuration(long millis) {
    millis /= 1000;
    long seconds = millis % 60;
    millis /= 60;
    long minutes = millis % 60;
    millis /= 60;
    long hours = millis;
    
    String formatted = ":" + StringUtils.leftPad(String.valueOf(seconds), 2, '0');
    String stringMinutes = String.valueOf(minutes);
    if (hours > 0) {
      return hours + ":" + StringUtils.leftPad(stringMinutes, 2, '0') + formatted;
    }
    return stringMinutes + formatted;
  }

  private ApiUser getApiUser(CurrentPlayer currentPlayer, OsuUser osuUser) {
    Future<ApiUser> request =
        exec.submit(() -> getApiUserTransient(currentPlayer.getId(), osuUser.getGameMode(),
            60 * 1000L));
    try {
      return request.get(500, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      request =
          exec.submit(() -> getApiUserTransient(currentPlayer.getId(), osuUser.getGameMode(), 0));
      try {
        return request.get(500, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e1) {
        return null;
      }
    }
  }

  private ApiUser getApiUserTransient(@UserId int userid, @GameMode int gameMode, long maxAge)
      throws IOException {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      ApiUser userData = api.getUserData(userid, gameMode, pm, maxAge);
      if (userData != null) {
        pm.makeTransient(userData);
      }
      return userData;
    } finally {
      pm.close();
    }
  }
}
