package me.reddev.osucelebrity.core;

public class MockClock implements Clock {
  long time = 0;
  
  @Override
  public long getTime() {
    return time;
  }

  @Override
  public void sleepUntil(long time) throws InterruptedException {
    if(time > this.time) {
      this.time = time;
    }
  }

  public void setTime(long time) {
    this.time = time;
  }
}
