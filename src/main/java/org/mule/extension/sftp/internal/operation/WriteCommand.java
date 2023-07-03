/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.connection.FileSystem;

import java.io.InputStream;

/**
 * Command design pattern for writing files
 *
 * @since 1.0
 */
public interface WriteCommand {

  String IS_A_DIRECTORY_MESSAGE = "Is a directory";

  /**
   * Writes a file under the considerations of {@link FileSystem#write(String, InputStream, FileWriteMode, boolean, boolean)}
   *
   * @param filePath              the path of the file to be written
   * @param content               the content to be written into the file
   * @param mode                  a {@link FileWriteMode}
   * @param lock                  whether or not to lock the file
   * @param createParentDirectory whether or not to attempt creating the parent directory if it doesn't exist.
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectory);
}
