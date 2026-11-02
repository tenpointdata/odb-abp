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

  private long calculate(long memoryLimit, String memoryLeft) {
    return initializer.calculateMemoryLeft(memoryLimit, "memory.leftToOS", memoryLeft);
  }
}
