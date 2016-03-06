package me.reddev.osucelebrity.core.api;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.Vote;
import org.mapstruct.Mapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/votes")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class VoteService {
  @Mapper
  public interface VoteMapper {
    public DisplayVote voteToDisplayVote(Vote vote);
  }

  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;
  
  /**
   * Shows the votes for the current player.
   * @return List of unique votes for the current player
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<DisplayVote> votes() {
    PersistenceManager pm = pmf.getPersistenceManager();
    
    try {
      QueuedPlayer queued = spectator.getCurrentPlayer(pm);
      if (queued == null) {
        return Collections.emptyList();
      }
      
      List<DisplayVote> votes =
          spectator.getVotes(pm, queued).stream().map(new VoteMapperImpl()::voteToDisplayVote)
              .sorted(Comparator.comparing(DisplayVote::getVoteTime)).collect(Collectors.toList());
      return votes;
    } finally {
      pm.close();
    }
  }
}
