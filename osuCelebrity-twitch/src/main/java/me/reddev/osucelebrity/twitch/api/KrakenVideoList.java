package me.reddev.osucelebrity.twitch.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties({"_links", "_total"})
public class KrakenVideoList {
  private List<KrakenVideo> videos;
}
