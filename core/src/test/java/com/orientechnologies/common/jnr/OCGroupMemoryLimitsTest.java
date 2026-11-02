package com.orientechnologies.common.jnr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OCGroupMemoryLimitsTest {
  private static final long GB = 1024L * 1024 * 1024;

  private static final String V1_MOUNTS =
      "cgroup /sys/fs/cgroup/cpu,cpuacct cgroup rw,nosuid,cpu,cpuacct 0 0\n"
          + "cgroup /sys/fs/cgroup/memory cgroup rw,nosuid,nodev,noexec,memory 0 0\n";
  private static final String V2_MOUNTS =
      "proc /proc proc rw,nosuid 0 0\n"
          + "cgroup2 /sys/fs/cgroup cgroup2 rw,nosuid,nodev,noexec,relatime 0 0\n";

  private Path root;

  @Before
  public void before() throws IOException {
    root = Files.createTempDirectory("cgroup-test");
  }

  @After
  public void after() throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }
  }

  @Test
  public void noCGroupFileMeansNoLimits() {
    assertThat(detect()).isNull();
  }

  @Test
  public void v1HostProcessReadsItsOwnCGroup() throws IOException {
    write("proc/self/cgroup", "5:cpu,cpuacct:/user.slice\n4:memory:/user.slice\n");
    write("proc/mounts", V1_MOUNTS);
    write("sys/fs/cgroup/memory/user.slice/memory.limit_in_bytes", String.valueOf(4 * GB));
    write("sys/fs/cgroup/memory/user.slice/memory.soft_limit_in_bytes", String.valueOf(3 * GB));

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(1);
    assertThat(result.insideContainer).isFalse();
    assertThat(result.hardLimit).isEqualTo(4 * GB);
    assertThat(result.softLimit).isEqualTo(3 * GB);
  }

  @Test
  public void v1ContainerFallsBackToMountRoot() throws IOException {
    write("proc/self/cgroup", "4:memory:/docker/0123456789abcdef\n");
    write("proc/mounts", V1_MOUNTS);
    write("sys/fs/cgroup/memory/memory.limit_in_bytes", String.valueOf(2 * GB));
    // the v1 "unlimited" value overflows a long and means "not set"
    write("sys/fs/cgroup/memory/memory.soft_limit_in_bytes", "18446744073709551615");

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(1);
    assertThat(result.insideContainer).isTrue();
    assertThat(result.hardLimit).isEqualTo(2 * GB);
    assertThat(result.softLimit).isEqualTo(OCGroupMemoryLimits.NOT_SET);
  }

  @Test
  public void v1UsesDefaultMountWhenNotListed() throws IOException {
    write("proc/self/cgroup", "4:memory:/\n");
    write("proc/mounts", "proc /proc proc rw 0 0\n");
    write("sys/fs/cgroup/memory/memory.limit_in_bytes", String.valueOf(GB));

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(1);
    assertThat(result.hardLimit).isEqualTo(GB);
  }

  @Test
  public void hybridModePrefersV1MemoryController() throws IOException {
    write("proc/self/cgroup", "4:memory:/user.slice\n0::/user.slice\n");
    write("proc/mounts", V1_MOUNTS + V2_MOUNTS);
    write("sys/fs/cgroup/memory/user.slice/memory.limit_in_bytes", String.valueOf(GB));

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(1);
    assertThat(result.hardLimit).isEqualTo(GB);
  }

  @Test
  public void v2ContainerWithPrivateNamespaceIsDetected() throws IOException {
    // Docker/Kubernetes on cgroup v2 default to a private cgroup namespace: the process sees
    // itself at "/" and the mount root is its container's cgroup, which carries the limits.
    write("proc/self/cgroup", "0::/\n");
    write("proc/mounts", V2_MOUNTS);
    write("sys/fs/cgroup/memory.max", String.valueOf(2 * GB));
    write("sys/fs/cgroup/memory.high", "max");

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(2);
    assertThat(result.insideContainer).isTrue();
    assertThat(result.hardLimit).isEqualTo(2 * GB);
    assertThat(result.softLimit).isEqualTo(OCGroupMemoryLimits.NOT_SET);
  }

  @Test
  public void v2ContainerWithHostNamespaceFallsBackToMountRoot() throws IOException {
    write("proc/self/cgroup", "0::/system.slice/docker-0123456789abcdef.scope\n");
    write("proc/mounts", V2_MOUNTS);
    write("sys/fs/cgroup/memory.max", String.valueOf(3 * GB));
    write("sys/fs/cgroup/memory.high", String.valueOf(2 * GB));

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(2);
    assertThat(result.insideContainer).isTrue();
    assertThat(result.hardLimit).isEqualTo(3 * GB);
    assertThat(result.softLimit).isEqualTo(2 * GB);
  }

  @Test
  public void v2HostRootCGroupHasNoLimits() throws IOException {
    // the real root cgroup has no memory.max/memory.high files
    write("proc/self/cgroup", "0::/\n");
    write("proc/mounts", V2_MOUNTS);
    write("sys/fs/cgroup/cgroup.controllers", "cpu io memory pids");

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(2);
    assertThat(result.insideContainer).isFalse();
    assertThat(result.hardLimit).isEqualTo(OCGroupMemoryLimits.NOT_SET);
    assertThat(result.softLimit).isEqualTo(OCGroupMemoryLimits.NOT_SET);
  }

  @Test
  public void v2SystemdServiceOnHostIsNotAContainer() throws IOException {
    write("proc/self/cgroup", "0::/system.slice/orientdb.service\n");
    write("proc/mounts", V2_MOUNTS);
    write("sys/fs/cgroup/system.slice/orientdb.service/memory.max", String.valueOf(8 * GB));
    write("sys/fs/cgroup/system.slice/orientdb.service/memory.high", "max");

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(2);
    assertThat(result.insideContainer).isFalse();
    assertThat(result.hardLimit).isEqualTo(8 * GB);
  }

  @Test
  public void malformedLinesAreIgnored() throws IOException {
    write("proc/self/cgroup", "garbage\n\n0::/\n");
    write("proc/mounts", "short line\n\n" + V2_MOUNTS);
    write("sys/fs/cgroup/memory.max", "not-a-number");

    final OCGroupMemoryLimits.Result result = detect();

    assertThat(result.version).isEqualTo(2);
    assertThat(result.hardLimit).isEqualTo(OCGroupMemoryLimits.NOT_SET);
  }

  private OCGroupMemoryLimits.Result detect() {
    return new OCGroupMemoryLimits(root).detect(true);
  }

  private void write(final String relativePath, final String content) throws IOException {
    final Path file = root.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
  }
}
