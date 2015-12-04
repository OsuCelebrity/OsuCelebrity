package me.reddev.osucelebrity.core.api;

import static me.reddev.osucelebrity.core.QVote.vote;

import com.querydsl.jdo.JDOQuery;
import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.core.QueuedPlayer;
import me.reddev.osucelebrity.core.Spectator;
import me.reddev.osucelebrity.core.Vote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
  private final PersistenceManagerFactory pmf;

  private final Spectator spectator;
  
  /**
   * Shows the votes for the current player.
   * @return List of unique votes for the current player
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Vote> votes() {
    PersistenceManager pm = pmf.getPersistenceManager();
    
    try {
      QueuedPlayer queued = spectator.getCurrentPlayer(pm);
      if (queued == null) {
        return Collections.<Vote>emptyList();
      }
      
      JDOQuery<Vote> query =
          new JDOQuery<>(pm).select(vote).from(vote)
              .where(vote.reference.eq(queued));
      
      //Create a modifiable result list
      List<Vote> votes = new ArrayList<Vote>();
      votes.addAll(query.fetchResults().getResults());   
      return getUniqueVotes(votes);
    } catch (Exception e) {
      throw e;
    } finally {
      pm.close();
    }
  }
  
  /**
   * Sorts a list of votes into unique (by user) and sorted by vote time desc.
   * @param votes The unsorted list of votes
   * @return The modified sorted list
   */
  private List<Vote> getUniqueVotes(List<Vote> votes) {
    //Compare in descending order
    votes.sort((d1, d2) -> Long.compare(d1.getVoteTime(), d2.getVoteTime()));
    Collections.reverse(votes);
    
    //Unique set of names
    Set<String> names = new HashSet<String>();
    for (Iterator<Vote> iterator = votes.iterator(); iterator.hasNext(); ) {
      Vote vote = iterator.next();
      
      if (!names.add(vote.getTwitchUser())) {
        iterator.remove();
      }
    }
    
    return votes;
  }
}
