/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;

import static java.lang.String.format;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.sftp.internal.command.SftpCommand;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Abstract implementation of {@link SftpCopyDelegate} for copying operations which require to FTP connections, one for reading the
 * source file and another for writing into the target path
 *
 * @since 1.0
 */
public abstract class AbstractSftpCopyDelegate implements SftpCopyDelegate {

  private final SftpCommand command;
  private final SftpFileSystem fileSystem;
  private final Logger LOGGER = LoggerFactory.getLogger(AbstractSftpCopyDelegate.class);

  /**
   * Creates new instance
   *
   * @param command the {@link SftpCommand} which requested this operation
   * @param fileSystem the {@link SftpFileSystem} which connects to the remote server
   */
  public AbstractSftpCopyDelegate(SftpCommand command, SftpFileSystem fileSystem) {
    this.command = command;
    this.fileSystem = fileSystem;
  }

  /**
   * Performs a recursive copy
   *  @param config the config which is parameterizing this operation
   * @param source the {@link FileAttributes} for the file to be copied
   * @param targetUri the {@link URI} to the target destination
   * @param overwrite whether to overwrite existing target paths
   */
  @Override
  public void doCopy(FileConnectorConfig config, FileAttributes source, URI targetUri, boolean overwrite) {
    ConnectionHandler<SftpFileSystem> writerConnectionHandler;
    final SftpFileSystem writerConnection;
    try {
      writerConnectionHandler = getWriterConnection(config);
      writerConnection = writerConnectionHandler.getConnection();
    } catch (ConnectionException e) {
      throw command
          .exception(format("FTP Copy operations require the use of two FTP connections. An exception was found trying to obtain second connection to"
              + "copy the path '%s' to '%s'", source.getPath(), targetUri.getPath()), e);
    }
    try {
      if (source.isDirectory()) {
        copyDirectory(config, URI.create(source.getPath()), targetUri, overwrite, writerConnection);
        LOGGER.trace("Copied directory {} to {}", source.getPath(), targetUri);
      } else {
        copyFile(config, source, targetUri, overwrite, writerConnection);
        LOGGER.trace("Copied file {} to {}", source.getPath(), targetUri);
      }
    } catch (ModuleException e) {
      throw e;
    } catch (Exception e) {
      throw command.exception(format("Found exception copying file '%s' to '%s'", source, targetUri.getPath()), e);
    } finally {
      writerConnectionHandler.release();
    }
  }

  /**
   * Performs a recursive copy of a directory
   *  @param config the config which is parameterizing this operation
   * @param sourceUri the path to the directory to be copied
   * @param target the target path
   * @param overwrite whether to overwrite the target files if they already exists
   * @param writerConnection the {@link SftpFileSystem} which connects to the target endpoint
   */
  protected abstract void copyDirectory(FileConnectorConfig config, URI sourceUri, URI target, boolean overwrite,
                                        SftpFileSystem writerConnection);

  /**
   * Copies one individual file
   *  @param config the config which is parameterizing this operation
   * @param source the {@link FileAttributes} for the file to be copied
   * @param target the target uri
   * @param overwrite whether to overwrite the target files if they already exists
   * @param writerConnection the {@link SftpFileSystem} which connects to the target endpoint
   */
  protected void copyFile(FileConnectorConfig config, FileAttributes source, URI target, boolean overwrite,
                          SftpFileSystem writerConnection) {
    FileAttributes targetFile = command.getFile(target.getPath());
    if (targetFile != null) {
      if (overwrite) {
        fileSystem.delete(targetFile.getPath());
      } else {
        throw command.alreadyExistsException(target);
      }
    }

    try (InputStream inputStream = fileSystem.retrieveFileContent(source)) {
      if (inputStream == null) {
        throw command
            .exception(format("Could not read file '%s' while trying to copy it to remote path '%s'", source.getPath(), target));
      }

      writeCopy(config, target.getPath(), inputStream, overwrite, writerConnection);
    } catch (Exception e) {
      throw command
          .exception(format("Found exception while trying to copy file '%s' to remote path '%s'", source.getPath(), target), e);
    }
  }

  private void writeCopy(FileConnectorConfig config, String targetPath, InputStream inputStream, boolean overwrite,
                         SftpFileSystem writerConnection)
      throws IOException {
    final FileWriteMode mode = overwrite ? FileWriteMode.OVERWRITE : FileWriteMode.CREATE_NEW;
    writerConnection.write(targetPath, inputStream, mode, false, true);
  }

  private ConnectionHandler<SftpFileSystem> getWriterConnection(FileConnectorConfig config) throws ConnectionException {
    return ((SftpConnector) config).getConnectionManager().getConnection(config);
  }
}
