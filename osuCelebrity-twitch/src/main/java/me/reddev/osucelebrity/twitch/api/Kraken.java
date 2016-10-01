package me.reddev.osucelebrity.twitch.api;

import me.reddev.osucelebrity.twitchapi.TwitchApiUser;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
public interface Kraken {
  @Path("/users/{user}")
  @GET
  TwitchApiUser getUser(@PathParam("user") String user);

  @Path("/channels/{channel}")
  KrakenChannel getChannel(@PathParam("channel") String channel);
}
