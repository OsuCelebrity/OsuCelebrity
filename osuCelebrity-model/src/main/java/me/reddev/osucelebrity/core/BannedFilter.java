package me.reddev.osucelebrity.core;

import lombok.Getter;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class BannedFilter {
  @PrimaryKey
  private long id;
  
  @Getter
  private String startsWith;

  public BannedFilter(String startsWith) {
    super();
    this.startsWith = startsWith;
  }
}
