package me.reddev.osucelebrity.core;

public class SystemClock implements Clock {

  @Override
  public long getTime() {
    return System.currentTimeMillis();
  }

  @Override
  public void sleepUntil(long time) throws InterruptedException {
    long millis = time - System.currentTimeMillis();
    if (millis > 0) {
      Thread.sleep(millis);
    }
  }
}
