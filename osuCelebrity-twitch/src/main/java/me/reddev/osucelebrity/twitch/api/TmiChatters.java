package me.reddev.osucelebrity.twitch.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmiChatters {
  @XmlElement(name = "chatter_count")
  private int chatterCount;

  private TmiChatterLists chatters;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TmiChatterLists {
    private List<String> moderators;

    private List<String> staff;

    private List<String> admins;

    @XmlElement(name = "global_mods")
    private List<String> globalMods;

    private List<String> viewers;
  }
}
