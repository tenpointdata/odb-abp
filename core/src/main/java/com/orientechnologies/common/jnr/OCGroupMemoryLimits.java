/*
 *
 *  *  Copyright 2010-2018 OrientDB LTD (http://orientdb.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://orientdb.com
 *
 */
package com.orientechnologies.common.jnr;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.log.OLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Detects the memory limits that Linux control groups impose on the current process.
 *
 * <p>Both the legacy cgroup v1 {@code memory} controller and the unified cgroup v2 hierarchy are
 * supported. cgroup v2 is the default for current container runtimes and distributions (Docker
 * 20.10+, Kubernetes 1.25+, RHEL 9, Ubuntu 21.10+), where a v1-only reader finds no memory
 * controller and therefore can't tell that the process is running inside a container.
 *
 * <p>All paths are resolved against a configurable filesystem root so the parsing can be tested
 * without a real {@code /proc} and {@code /sys}.
 */
final class OCGroupMemoryLimits {
  static final long NOT_SET = -1;

  private static final OLogger logger = OLogManager.instance().logger(OCGroupMemoryLimits.class);

  private static final String DEFAULT_V1_MEMORY_MOUNT = "/sys/fs/cgroup/memory";
  private static final String DEFAULT_V2_MOUNT = "/sys/fs/cgroup";

  private final Path root;

  OCGroupMemoryLimits(final Path root) {
    this.root = root;
  }

  /** Memory limits read from the cgroup the current process belongs to. */
  static final class Result {
    final int version;
    final Path cgroupDir;
    final long softLimit;
    final long hardLimit;
    final boolean insideContainer;

    Result(
        final int version,
        final Path cgroupDir,
        final long softLimit,
        final long hardLimit,
        final boolean insideContainer) {
      this.version = version;
      this.cgroupDir = cgroupDir;
      this.softLimit = softLimit;
      this.hardLimit = hardLimit;
      this.insideContainer = insideContainer;
    }
  }

  /**
   * @return the limits of the memory cgroup of the current process, or {@code null} if the process
   *     doesn't belong to a memory cgroup (non-Linux OS, or cgroups not mounted).
   */
  Result detect(final boolean printSteps) {
    final List<String> cgroups = readLines("/proc/self/cgroup");
    if (cgroups == null) {
      return null;
    }

    String v1MemoryPath = null;
    String v2Path = null;
    for (final String line : cgroups) {
      // hierarchy-ID:controller-list:cgroup-path
      final String[] parts = line.split(":", 3);
      if (parts.length < 3) {
        continue;
      }
      if (parts[1].equals("memory")) {
        v1MemoryPath = parts[2];
      } else if (parts[0].equals("0") && parts[1].isEmpty()) {
        v2Path = parts[2];
      }
    }

    // In hybrid mode a process is listed in both hierarchies; the memory controller is attached
    // to v1 when it has its own line there.
    if (v1MemoryPath != null) {
      return detectV1(v1MemoryPath, printSteps);
    }
    if (v2Path != null) {
      return detectV2(v2Path, printSteps);
    }
    return null;
  }

  private Result detectV1(final String cgroupPath, final boolean printSteps) {
    if (printSteps) {
      logger.infoNoDb("Path to 'memory' cgroup is '%s'", cgroupPath);
    }

    String mountPoint = findMount("cgroup", "memory");
    if (mountPoint == null) {
      mountPoint = DEFAULT_V1_MEMORY_MOUNT;
    }
    if (printSteps) {
      logger.infoNoDb("Mounting path for memory cgroup controller is '%s'", mountPoint);
    }

    final Path mountDir = resolve(mountPoint);
    Path cgroupDir = mountDir.resolve(relative(cgroupPath));
    boolean insideContainer = false;
    if (!Files.exists(cgroupDir)) {
      if (printSteps) {
        logger.infoNoDb(
            "Can not find '%s' path for memory cgroup, it is supposed that process is"
                + " running in container, will try to read root '%s' memory cgroup data",
            cgroupDir, mountDir);
      }
      cgroupDir = mountDir;
      insideContainer = true;
    }

    final long soft = readLimit(cgroupDir, "memory.soft_limit_in_bytes", "soft", printSteps);
    final long hard = readLimit(cgroupDir, "memory.limit_in_bytes", "hard", printSteps);
    return new Result(1, cgroupDir, soft, hard, insideContainer);
  }

  private Result detectV2(final String cgroupPath, final boolean printSteps) {
    if (printSteps) {
      logger.infoNoDb("Path to unified (v2) cgroup is '%s'", cgroupPath);
    }

    String mountPoint = findMount("cgroup2", null);
    if (mountPoint == null) {
      mountPoint = DEFAULT_V2_MOUNT;
    }
    if (printSteps) {
      logger.infoNoDb("Mounting path for unified (v2) cgroup hierarchy is '%s'", mountPoint);
    }

    final Path mountDir = resolve(mountPoint);
    Path cgroupDir = mountDir.resolve(relative(cgroupPath));
    boolean insideContainer;
    if (!Files.exists(cgroupDir)) {
      // The cgroup path is relative to the host hierarchy, but only the container's own subtree
      // is mounted: the process is in a container without a private cgroup namespace.
      if (printSteps) {
        logger.infoNoDb(
            "Can not find '%s' path for cgroup, it is supposed that process is"
                + " running in container, will try to read root '%s' cgroup data",
            cgroupDir, mountDir);
      }
      cgroupDir = mountDir;
      insideContainer = true;
    } else {
      // The real root cgroup has no memory.max file. Seeing one at "/" means the process is in a
      // private cgroup namespace whose root is its container's cgroup.
      insideContainer =
          relative(cgroupPath).isEmpty() && Files.exists(cgroupDir.resolve("memory.max"));
    }

    // memory.high is the throttling threshold, the closest v2 equivalent of the v1 soft limit
    final long soft = readLimit(cgroupDir, "memory.high", "soft", printSteps);
    final long hard = readLimit(cgroupDir, "memory.max", "hard", printSteps);
    return new Result(2, cgroupDir, soft, hard, insideContainer);
  }

  /**
   * Finds the mount point of a cgroup filesystem in {@code /proc/mounts}.
   *
   * @param fsType "cgroup" for v1 controllers, "cgroup2" for the unified hierarchy.
   * @param lastPathSegment if not null, the mount point must end with this segment (used to pick
   *     the v1 hierarchy of a given controller).
   */
  private String findMount(final String fsType, final String lastPathSegment) {
    final List<String> mounts = readLines("/proc/mounts");
    if (mounts == null) {
      return null;
    }
    String found = null;
    for (final String mount : mounts) {
      // device mount-point fs-type options dump pass
      final String[] parts = mount.trim().split("\\s+");
      if (parts.length < 3 || !parts[2].equals(fsType)) {
        continue;
      }
      final String mountPoint = parts[1];
      if (lastPathSegment == null) {
        return mountPoint;
      }
      final String[] segments = mountPoint.split("/");
      if (segments.length > 0 && segments[segments.length - 1].equals(lastPathSegment)) {
        found = mountPoint;
      }
    }
    return found;
  }

  /**
   * Reads a limit file. Returns {@link #NOT_SET} when the file is missing, holds the v2 "max"
   * keyword, or holds a v1 "unlimited" value too large for a long.
   */
  private long readLimit(
      final Path cgroupDir, final String fileName, final String kind, final boolean printSteps) {
    final Path file = cgroupDir.resolve(fileName);
    if (!Files.exists(file)) {
      if (printSteps) {
        logger.infoNoDb("Can not read memory %s limit for cgroup '%s'", kind, cgroupDir);
      }
      return NOT_SET;
    }

    final String value;
    try {
      final List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);
      value = lines.isEmpty() ? "" : lines.get(0).trim();
    } catch (final IOException | RuntimeException e) {
      logger.errorNoDb("Can not read memory %s limit for cgroup '%s'", e, kind, cgroupDir);
      return NOT_SET;
    }

    if (value.equals("max")) {
      if (printSteps) {
        logger.infoNoDb("cgroup %s memory limit is not set", kind);
      }
      return NOT_SET;
    }

    try {
      final long limit = Long.parseLong(value);
      if (printSteps) {
        logger.infoNoDb(
            "cgroup %s memory limit is %d B/%d MB/%d GB",
            kind, limit, limit / (1024 * 1024), limit / (1024 * 1024 * 1024));
      }
      return limit;
    } catch (final NumberFormatException nfe) {
      if (value.matches("\\d+")) {
        if (printSteps) {
          logger.infoNoDb("cgroup %s memory limit is not set", kind);
        }
      } else {
        logger.errorNoDb("Can not read memory %s limit for cgroup '%s'", nfe, kind, cgroupDir);
      }
      return NOT_SET;
    }
  }

  private List<String> readLines(final String absolutePath) {
    final Path file = resolve(absolutePath);
    if (!Files.isReadable(file)) {
      return null;
    }
    try {
      return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (final IOException | RuntimeException e) {
      logger.warnNoDb(
          "Can not read '%s' (%s), no restrictions applied by cgroups will be taken into account",
          absolutePath, e);
      return null;
    }
  }

  private Path resolve(final String absolutePath) {
    return root.resolve(relative(absolutePath));
  }

  private static String relative(final String absolutePath) {
    int start = 0;
    while (start < absolutePath.length() && absolutePath.charAt(start) == '/') {
      start++;
    }
    return absolutePath.substring(start);
  }
}
