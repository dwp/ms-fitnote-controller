package uk.gov.dwp.health.fitnotecontroller.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryChecker {
  private static final Logger LOG = LoggerFactory.getLogger(MemoryChecker.class.getName());
  private static final long MEGABYTE = 1024L * 1024L;

  private MemoryChecker() {
    // prevent instantiation
  }

  public static boolean hasEnoughMemoryForRequest(Runtime javaRunTime, int requiredMemoryMb) {
    long freeMemoryMb = returnCurrentAvailableMemoryInMb(javaRunTime);
    int absoluteMemory = Math.abs(requiredMemoryMb);
    boolean isEnoughMemory = freeMemoryMb > absoluteMemory;

    LOG.info(
        "Current available free memory is {}mb, total memory {}mb, "
        + "max memory {}mb, "
        + "abs required memory is {}mb, request allowed = {}",
        freeMemoryMb,
        javaRunTime.totalMemory() / MEGABYTE,
        javaRunTime.maxMemory() / MEGABYTE,
        absoluteMemory,
        isEnoughMemory);
    return freeMemoryMb >= absoluteMemory;
  }

  public static long returnCurrentAvailableMemoryInMb(Runtime javaRunTime) {
    return javaRunTime.freeMemory() / MEGABYTE;
  }
}
