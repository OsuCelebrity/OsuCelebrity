package me.reddev.osucelebrity.twitchapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;

import javax.annotation.CheckForNull;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.bind.annotation.XmlElement;

@Data
@PersistenceCapable
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitchApiUser {
  @XmlElement(name = "display_name")
  private String displayName;

  @XmlElement(name = "_id")
  @PrimaryKey
  private int id;

  @Index
  private String name;

  private String type;
  /*
   * "fewer than 300 characters"
   */
  @CheckForNull
  @Column(length = 1000)
  private String bio;

  @XmlElement(name = "created_at")
  private Date createdAt;

  @XmlElement(name = "updated_at")
  private Date updatedAt;

  @CheckForNull
  private String logo;
  private long downloaded;
}
