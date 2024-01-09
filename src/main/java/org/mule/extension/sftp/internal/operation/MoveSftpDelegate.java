/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static java.lang.String.format;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveSftpDelegate implements SftpCopyDelegate {

  private SftpCommand command;
  private SftpFileSystemConnection fileSystem;
  private static final Logger LOGGER = LoggerFactory.getLogger(MoveSftpDelegate.class);

  public MoveSftpDelegate(SftpCommand command, SftpFileSystemConnection fileSystem) {
    this.command = command;
    this.fileSystem = fileSystem;
  }

  @Override
  public void doCopy(FileConnectorConfig config, FileAttributes source, URI targetUri, boolean overwrite) {
    String path = source.getPath();
    try {
      if (command.exists(targetUri)) {
        if (overwrite) {
          fileSystem.delete(targetUri.getPath());
        } else {
          command.alreadyExistsException(targetUri);
        }
      }

      command.rename(path, targetUri.getPath(), overwrite);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Moved file {} to {}", path, targetUri.getPath());
      }
    } catch (ModuleException e) {
      LOGGER.error("Error trying to move file {} to {}", path, targetUri.getPath(), e);
      throw e;
    } catch (Exception e) {
      LOGGER.error("Error trying to move file {} to {}", path, targetUri.getPath(), e);
      throw command.exception(format("Found exception moving file '%s' to '%s'", path, targetUri.getPath()), e);
    }
  }
}
