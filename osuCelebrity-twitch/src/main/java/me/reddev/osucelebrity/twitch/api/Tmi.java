package me.reddev.osucelebrity.twitch.api;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
public interface Tmi {
  @Path("group/user/{user}")
  TmiUser getUser(@PathParam("user") String user);
}
