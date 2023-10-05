/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import static java.lang.Thread.currentThread;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility class for normalizing FTP paths
 *
 * @since 1.0
 */
public class SftpUtils {

  private static final Pattern TRIM_END_SLASH_PATTERN = Pattern.compile("/+$");

  private SftpUtils() {}

  /**
   * @param path to be normalized
   * @return a {@link String} representing the path in the following format (using the unix path separator):
   *         "directory/subdirectory"
   */
  public static String normalizePath(String path) {
    String pathToNormalize = TRIM_END_SLASH_PATTERN.matcher(path).replaceFirst("");
    return FilenameUtils.normalize(pathToNormalize, true);
  }

  public static String resolvePathOrResource(String pathOrResourceName) {
    URL resource = currentThread().getContextClassLoader().getResource(pathOrResourceName);
    return resource != null ? resource.getPath() : pathOrResourceName;
  }

  public static ZonedDateTime asDateTime(Instant instant) {
    return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
  }
}
