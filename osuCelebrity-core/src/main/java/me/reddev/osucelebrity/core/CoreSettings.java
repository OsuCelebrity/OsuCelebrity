package me.reddev.osucelebrity.core;

public interface CoreSettings {
  long getDefaultSpecDuration();
  
  long getNextPlayerNotifyTime();
  
  int getApiPort();
  
  long getStreamDelay();

  long getVoteWindow();
  
  long getAutoSpecTime();
  
  long getOfflineTimeout();
  
  long getIdleTimeout();
  
  long getAutoSpecMaxRank();
  
  long getAutoSpecMaxLastActivity();
}
