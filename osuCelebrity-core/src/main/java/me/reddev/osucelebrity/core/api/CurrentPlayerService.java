package me.reddev.osucelebrity.core.api;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.reddev.osucelebrity.core.CoreSettings;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.osu.Osu;
import me.reddev.osucelebrity.osu.OsuStatus;
import me.reddev.osucelebrity.osu.OsuStatus.Type;
import me.reddev.osucelebrity.osuapi.ApiUser;
import me.reddev.osucelebrity.osuapi.OsuApi;

import org.tillerino.osuApiModel.GameModes;
import org.tillerino.osuApiModel.types.GameMode;
import org.tillerino.osuApiModel.types.UserId;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

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

    String nextPlayer;
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
      QueuedPlayer player = spectator.getCurrentPlayer(pm);
      if (player != null) {
        response.setName(player.getPlayer().getUserName());
        {
          Duration duration = Duration.ofMillis(System.currentTimeMillis() - player.getStartedAt());
          LocalTime localTime = LocalTime.MIDNIGHT.plus(duration);
          response.setPlayingFor(DateTimeFormatter.ofPattern("m:ss").format(localTime));
        }
        long timeLeft = Math.max(0, player.getStoppingAt() - player.getLastRemainingTimeUpdate());
        response.setHealth(timeLeft / (double) coreSettings.getDefaultSpecDuration());
        response.setId(player.getPlayer().getUserId());

        ApiUser apiUser = getApiUser(response);

        if (apiUser != null) {
          response.rank = apiUser.getRank();
          response.pp = apiUser.getPp();
          response.playCount = apiUser.getPlayCount();
          response.level = apiUser.getLevel();
          response.accuracy = apiUser.getAccuracy();
          response.country = apiUser.getCountry();
        }
      }
      OsuStatus clientStatus = osu.getClientStatus();
      if (clientStatus != null && clientStatus.getType() == Type.PLAYING) {
        response.setBeatmap(clientStatus.getDetail());
      }
      QueuedPlayer nextPlayer = spectator.getNextPlayer(pm);
      if (nextPlayer != null) {
        response.nextPlayer = nextPlayer.getPlayer().getUserName();
      }
      return response;
    } finally {
      pm.close();
    }
  }

  private ApiUser getApiUser(CurrentPlayer currentPlayer) {
    Future<ApiUser> request =
        exec.submit(() -> getApiUserTransient(currentPlayer.getId(), GameModes.OSU, 60 * 1000L));
    try {
      return request.get(500, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      request = exec.submit(() -> getApiUserTransient(currentPlayer.getId(), GameModes.OSU, 0));
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
