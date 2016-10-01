package me.reddev.osucelebrity.twitch.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public interface TmiUser {
  @Path("/chatters")
  @GET
  TmiChatters getChatters();
}
