/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SftpCommand} which implements the {@link CopyCommand} contract
 *
 * @since 1.0
 */
public class SftpCopyCommand extends SftpCommand implements CopyCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpCopyCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpCopyCommand(SftpFileSystemConnection fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void copy(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                   boolean createParentDirectories, String renameTo) {
    copy(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo,
         new SftpCopyDelegate(this, this.fileSystem));
  }

  private class SftpCopyDelegate extends AbstractSftpCopyDelegate {

    public SftpCopyDelegate(SftpCommand command, SftpFileSystemConnection fileSystem) {
      super(command, fileSystem);
    }

    @Override
    protected void copyDirectory(FileConnectorConfig config, URI sourceUri, URI target, boolean overwrite,
                                 SftpFileSystemConnection writerConnection) {
      for (FileAttributes fileAttributes : client.list(sourceUri.getPath())) {
        String path = fileAttributes.getPath();
        if (isVirtualDirectory(fileAttributes.getName())) {
          continue;
        }

        URI targetUri = createUri(target.getPath(), fileAttributes.getName());
        if (fileAttributes.isDirectory()) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Copy directory {} to {}", path, target);
          }
          copyDirectory(config, URI.create(path), targetUri, overwrite, writerConnection);
        } else {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Copy file {} to {}", path, target);
          }
          copyFile(config, fileAttributes, targetUri, overwrite, writerConnection);
        }
      }
    }
  }
}
