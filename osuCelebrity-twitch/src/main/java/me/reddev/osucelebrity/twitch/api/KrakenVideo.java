package me.reddev.osucelebrity.twitch.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrakenVideo {
  @XmlElement(name = "_id")
  private String id;

  @XmlElement(name = "broadcast_id")
  private long broadcastId;

  private String title;

  private double length;

  private String url;

  private String status;

  @XmlElement(name = "recorded_at")
  private Date recordedAt;

  @XmlElement(name = "created_at")
  private Date createdAt;

  @XmlElement(name = "delete_at")
  private Date deleteAt;
}
