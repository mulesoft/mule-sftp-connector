/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import java.io.Serializable;

/**
 * Canonical representation of a file's metadata attributes.
 * <p>
 * It contains information such as a file's name, size, timestamp, properties, etc.
 *
 * @since 1.0
 */
public interface FileAttributes extends Serializable {

  /**
   * @return The file size in bytes
   */
  long getSize();

  /**
   * @return {@code true} if the file is not a directory nor a symbolic link
   */
  boolean isRegularFile();

  /**
   * @return {@code true} if the file is a directory
   */
  boolean isDirectory();

  /**
   * @return {@code true} if the file is a symbolic link
   */
  boolean isSymbolicLink();

  /**
   * @return The file's path
   */
  String getPath();

  /**
   * @return The file's name
   */
  String getName();
}
