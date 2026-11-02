package com.orientechnologies.orient.distributed.context.coordination.topology;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class ONodeInfo {
  private final LongSupplier nanoTime;
  private volatile long lastReceivedNotification;

  public ONodeInfo(LongSupplier nanoTime) {
    this.nanoTime = nanoTime;
    lastReceivedNotification = nanoTime.getAsLong();
  }

  public void ping() {
    lastReceivedNotification = nanoTime.getAsLong();
  }

  public boolean awayMoreThan(long timeInMills) {
    // nanoTime values are compared by their difference, they can overflow
    return nanoTime.getAsLong() - lastReceivedNotification
        > TimeUnit.MILLISECONDS.toNanos(timeInMills);
  }
}
