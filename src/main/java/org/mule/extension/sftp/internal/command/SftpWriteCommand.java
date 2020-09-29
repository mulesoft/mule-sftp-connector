/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_PERMISSION_DENIED;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.jcraft.jsch.SftpException;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.command.WriteCommand;
import org.mule.extension.file.common.api.exceptions.DeletedFileWhileReadException;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.file.common.api.lock.NullUriLock;
import org.mule.extension.file.common.api.lock.UriLock;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;

/**
 * A {@link SftpCommand} which implements the {@link WriteCommand} contract
 *
 * @since 1.0
 */
public final class SftpWriteCommand extends SftpCommand implements WriteCommand {

  private static final Logger LOGGER = getLogger(SftpWriteCommand.class);
  private static final String WRITE_EXCEPTION_MESSAGE = "Exception was found writing to file '%s'";

  /**
   * {@inheritDoc}
   */
  public SftpWriteCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
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
      if (e instanceof SftpException && ((SftpException) e).id == SSH_FX_PERMISSION_DENIED) {
        throw new ModuleException(format(WRITE_EXCEPTION_MESSAGE, uri.getPath()), FileError.ACCESS_DENIED, e);
      }
      if (e.getCause() instanceof DeletedFileWhileReadException) {
        throw new ModuleException(WRITE_EXCEPTION_MESSAGE, FileError.FILE_DOESNT_EXIST, e.getCause());
      }
      Optional<ModuleException> exceptionFromAnotherModule = ExceptionUtils.extractOfType(e, ModuleException.class);
      if (exceptionFromAnotherModule.isPresent()) {
        throw exceptionFromAnotherModule.get();
      }
      throw exception(format(WRITE_EXCEPTION_MESSAGE, uri.getPath()), e);
    } finally {
      pathLock.release();
    }
  }

}
