package com.orientechnologies.orient.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class OMemoryAndLocalPaginatedEnginesInitializerTest {
  private static final long MB = 1024L * 1024;
  private static final long GB = 1024 * MB;

  private final OMemoryAndLocalPaginatedEnginesInitializer initializer =
      new OMemoryAndLocalPaginatedEnginesInitializer();

  @Test
  public void percentageIsAppliedToLargeMemoryLimits() {
    // used to be truncated to an int, capping the result to 2GB on any host with more memory
    assertThat(calculate(64 * GB, "10%")).isEqualTo((long) (64 * GB * 0.9));
    assertThat(calculate(16 * GB, "25%")).isEqualTo(12 * GB);
  }

  @Test
  public void absoluteValuesAreSubtracted() {
    assertThat(calculate(16 * GB, "2g")).isEqualTo(14 * GB);
    assertThat(calculate(16 * GB, "256m")).isEqualTo(16 * GB - 256 * MB);
    assertThat(calculate(16 * GB, "1024k")).isEqualTo(16 * GB - MB);
    assertThat(calculate(16 * GB, "4096b")).isEqualTo(16 * GB - 4096);
  }

  @Test
  public void invalidValuesLeaveTheLimitUnchanged() {
    assertThat(calculate(16 * GB, null)).isEqualTo(16 * GB);
    assertThat(calculate(16 * GB, "x")).isEqualTo(16 * GB);
    assertThat(calculate(16 * GB, "100%")).isEqualTo(16 * GB);
    assertThat(calculate(16 * GB, "abc%")).isEqualTo(16 * GB);
    assertThat(calculate(16 * GB, "12t")).isEqualTo(16 * GB);
  }

  @Test
  public void diskCacheLeavesHeapAndDirectMemoryPoolOut() {
    // a 64GB container with a 16GB heap and the default 2g left and 1GB pool
    assertThat(
            initializer.calculateDiskCacheSizeInMB(
                64 * GB, "memory.leftToContainer", "2g", 16 * GB, GB))
        .isEqualTo(45 * 1024);
    // too small: negative, the caller falls back to the minimal cache
    assertThat(
            initializer.calculateDiskCacheSizeInMB(2 * GB, "memory.leftToContainer", "2g", GB, GB))
        .isNegative();
  }

  private long calculate(long memoryLimit, String memoryLeft) {
    return initializer.calculateMemoryLeft(memoryLimit, "memory.leftToOS", memoryLeft);
  }
}
