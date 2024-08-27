/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import java.io.InputStream;

public class SftpCustomWriteDelegate implements SftpWriteDelegate {

  private final CustomWriteBufferSize customWriteBufferSize;

  public SftpCustomWriteDelegate(CustomWriteBufferSize customWriteBufferSize) {
    this.customWriteBufferSize = customWriteBufferSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(SftpFileSystemConnection fileSystem, String filePath, InputStream content, FileWriteMode mode, boolean lock,
                    boolean createParentDirectories) {
    fileSystem.write(filePath, content, mode, lock, createParentDirectories, customWriteBufferSize);
  }
}
