/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static org.mule.extension.sftp.api.util.UriUtils.createUri;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

/**
 * A {@link SftpCommand} which implements the {@link DeleteCommand} contract
 *
 * @since 1.0
 */
public final class SftpDeleteCommand extends SftpCommand implements DeleteCommand {

  private static final Logger LOGGER = getLogger(SftpDeleteCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpDeleteCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String filePath) {
    FileAttributes fileAttributes = getExistingFile(filePath);
    final boolean isDirectory = fileAttributes.isDirectory();
    final String path = fileAttributes.getPath();

    try {
      if (isDirectory) {
        deleteDirectory(path);
      } else {
        deleteFile(path);
      }
    } catch (Exception e) {
      LOGGER.error("Error deleting {}", path, e);
      throw e;
    }
  }

  private void deleteFile(String path) {
    fileSystem.verifyNotLocked(createUri(path));
    LOGGER.debug("Preparing to delete file '{}'", path);
    client.deleteFile(path);

    logDelete(path);
  }

  private void deleteDirectory(String path) {
    LOGGER.debug("Preparing to delete directory '{}'", path);
    for (FileAttributes file : client.list(path)) {
      final String filePath = file.getPath();
      if (isVirtualDirectory(file.getName())) {
        continue;
      }

      if (file.isDirectory()) {
        deleteDirectory(filePath);
      } else {
        deleteFile(filePath);
      }
    }

    URI directoryUri = createUri(path);
    String directoryFragment = FilenameUtils.getName(directoryUri.getPath());
    if (isVirtualDirectory(directoryFragment)) {
      path = directoryUri.getPath();
    }
    client.deleteDirectory(path);

    logDelete(path);
  }

  private void logDelete(String path) {
    LOGGER.debug("Successfully deleted '{}'", path);
  }
}
