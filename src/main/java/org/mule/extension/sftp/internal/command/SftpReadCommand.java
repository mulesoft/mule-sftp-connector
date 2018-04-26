/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ReadCommand;
import org.mule.extension.file.common.api.lock.NullPathLock;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.SftpConnector;
import org.mule.extension.sftp.internal.SftpInputStream;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A {@link SftpCommand} which implements the {@link ReadCommand} contract
 *
 * @since 1.0
 */
public final class SftpReadCommand extends SftpCommand implements ReadCommand<SftpFileAttributes> {

  /**
   * {@inheritDoc}
   */
  public SftpReadCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<InputStream, SftpFileAttributes> read(FileConnectorConfig config, String filePath, boolean lock) {
    return read(config, filePath, lock, null);
  }

  public Result<InputStream, SftpFileAttributes> read(FileConnectorConfig config, String filePath, boolean lock,
                                                      Long timeBetweenSizeCheck) {
    SftpFileAttributes attributes = getExistingFile(filePath);
    if (attributes.isDirectory()) {
      throw cannotReadDirectoryException(Paths.get(attributes.getPath()));
    }

    Path path = Paths.get(attributes.getPath());

    PathLock pathLock = lock ? fileSystem.lock(path) : new NullPathLock(path);
    InputStream payload = null;
    try {
      payload = SftpInputStream.newInstance((SftpConnector) config, attributes, pathLock, timeBetweenSizeCheck);
      MediaType resolvedMediaType = fileSystem.getFileMessageMediaType(attributes);
      return Result.<InputStream, SftpFileAttributes>builder().output(payload).mediaType(resolvedMediaType).attributes(attributes)
          .build();
    } catch (Exception e) {
      pathLock.release();
      IOUtils.closeQuietly(payload);
      throw exception("Could not fetch file " + path, e);
    }
  }

  public SftpFileAttributes readAttributes(String filePath) {
    return getFile(filePath);
  }
}
