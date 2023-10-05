/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility class for normalizing FTP paths
 *
 * @since 1.0
 */
public class SftpUtils {

  private SftpUtils() {}

  /**
   * @param path to be normalized
   * @return a {@link String} representing the path in the following format (using the unix path separator):
   *         "directory/subdirectory"
   */
  public static String normalizePath(String path) {
    String pathToNormalize = path;
    if (!isEmpty(path) && path.charAt(path.length() - 1) == '/') {
      pathToNormalize = path.substring(0, path.length() - 2);
    }
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
