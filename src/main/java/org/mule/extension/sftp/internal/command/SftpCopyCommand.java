/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static org.mule.extension.file.common.api.util.UriUtils.createUri;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.CopyCommand;
import org.mule.extension.sftp.internal.AbstractSftpCopyDelegate;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

/**
 * A {@link SftpCommand} which implements the {@link CopyCommand} contract
 *
 * @since 1.0
 */
public class SftpCopyCommand extends SftpCommand implements CopyCommand {

  /**
   * {@inheritDoc}
   */
  public SftpCopyCommand(SftpFileSystem fileSystem, SftpClient client) {
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

    public SftpCopyDelegate(SftpCommand command, SftpFileSystem fileSystem) {
      super(command, fileSystem);
    }

    protected void copyDirectory(FileConnectorConfig config, Path sourcePath, Path target, boolean overwrite,
                                 SftpFileSystem writerConnection) {
      for (FileAttributes fileAttributes : client.list(sourcePath.toString())) {
        if (isVirtualDirectory(fileAttributes.getName())) {
          continue;
        }

        if (fileAttributes.isDirectory()) {
          Path targetPath = target.resolve(fileAttributes.getName());
          sourcePath = Paths.get(fileAttributes.getPath());
        } else {
          target = target.resolve(fileAttributes.getName());
        }
      }
    }

    @Override
    protected void copyDirectory(FileConnectorConfig config, URI sourceUri, URI target, boolean overwrite,
                                 SftpFileSystem writerConnection) {
      copyDirectory(config, Paths.get(sourceUri.getPath()), Paths.get(target.getPath()), overwrite, writerConnection);
      for (FileAttributes fileAttributes : client.list(sourceUri.getPath())) {
        if (isVirtualDirectory(fileAttributes.getName())) {
          continue;
        }

        URI targetUri = createUri(target.getPath(), fileAttributes.getName());
        if (fileAttributes.isDirectory()) {
          copyDirectory(config, URI.create(fileAttributes.getPath()), targetUri, overwrite, writerConnection);
        } else {
          copyFile(config, fileAttributes, targetUri, overwrite, writerConnection);
        }
      }
    }
  }
}
