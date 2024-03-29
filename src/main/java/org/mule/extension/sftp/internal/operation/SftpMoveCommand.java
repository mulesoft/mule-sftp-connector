/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SftpCommand} which implements the {@link MoveCommand} contract
 *
 * @since 1.0
 */
public class SftpMoveCommand extends SftpCommand implements MoveCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpMoveCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpMoveCommand(SftpFileSystemConnection fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                   boolean createParentDirectories, String renameTo) {
    copy(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo, new MoveSftpDelegate(this, fileSystem));
    LOGGER.debug("Moved '{}' to '{}'", sourcePath, targetPath);
  }
}
