/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.exception.DeletedFileWhileReadException;
import org.mule.extension.sftp.internal.exception.FileAlreadyExistsException;
import org.mule.extension.sftp.internal.exception.FileDoesNotExistsException;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.extension.sftp.internal.lock.NullUriLock;
import org.mule.extension.sftp.internal.lock.UriLock;

import java.io.InputStream;
import java.net.URI;

import org.slf4j.Logger;

/**
 * A {@link SftpCommand} which implements the {@link WriteCommand} contract
 *
 * @since 1.0
 */
public final class SftpWriteCommand extends SftpCommand implements WriteCommand {

  private static final Logger LOGGER = getLogger(SftpWriteCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpWriteCommand(SftpFileSystemConnection fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String filePath, InputStream content, FileWriteMode mode,
                    boolean lock, boolean createParentDirectory) {
    URI uri = resolvePath(filePath);
    FileAttributes file = getFile(filePath);

    if (file == null) {
      assureParentFolderExists(uri, createParentDirectory);
    } else {
      if (mode == FileWriteMode.CREATE_NEW) {
        throw new FileAlreadyExistsException(format(
                                                    "Cannot write to path '%s' because it already exists and write mode '%s' was selected. "
                                                        + "Use a different write mode or point to a path which doesn't exist",
                                                    uri.getPath(), mode));
      }
    }

    UriLock pathLock = lock ? fileSystem.lock(uri) : new NullUriLock(uri);

    try {
      client.write(uri.getPath(), content, mode);
      LOGGER.debug("Successfully wrote to path {} mode {}", uri.getPath(), mode);
    } catch (Exception e) {
      LOGGER.error("Error writing to file {} mode {}", filePath, mode, e);
      if (e instanceof DeletedFileWhileReadException) {
        throw new FileDoesNotExistsException(e.getCause().getMessage(), e);
      }
      throw client.handleException(format("Exception was found writing to file '%s'", uri.getPath()), e);
    } finally {
      pathLock.release();
    }
  }

}
