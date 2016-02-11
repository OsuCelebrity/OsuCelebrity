package me.reddev.osucelebrity.twitchapi;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;

import javax.annotation.CheckForNull;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@Data
@PersistenceCapable
public class TwitchApiUser {
  @SerializedName("display_name")
  private String displayName;
  @SerializedName("_id")
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
  @SerializedName("created_at")
  private Date createdAt;
  @SerializedName("updated_at")
  private Date updatedAt;
  @CheckForNull
  private String logo;
  private long downloaded;
}
