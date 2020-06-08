/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.command.WriteCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.lock.NullUriLock;
import org.mule.extension.file.common.api.lock.UriLock;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
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
  public SftpWriteCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public void write(String filePath, InputStream content, FileWriteMode mode,
                    boolean lock, boolean createParentDirectory, String encoding) {
    write(filePath, content, mode, lock, createParentDirectory);
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
      LOGGER.debug("Successfully wrote to path {}", uri.getPath());
    } catch (Exception e) {
      throw exception(format("Exception was found writing to file '%s'", uri.getPath()), e);
    } finally {
      pathLock.release();
    }
  }

}
