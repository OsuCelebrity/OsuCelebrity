package me.reddev.osucelebrity.twitch.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;


public interface KrakenChannel {
  @GET
  @Path("/videos")
  KrakenVideoList getVideos(@QueryParam("broadcasts") boolean broadcasts,
      @QueryParam("limit") int limit);
}
