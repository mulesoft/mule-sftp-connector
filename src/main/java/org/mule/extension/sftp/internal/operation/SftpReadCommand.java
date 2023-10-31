/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.util.UriUtils;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.extension.sftp.internal.lock.NullUriLock;
import org.mule.extension.sftp.internal.lock.UriLock;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.net.URI;

/**
 * A {@link SftpCommand} which implements the {@link ReadCommand} contract
 *
 * @since 1.0
 */
public final class SftpReadCommand extends SftpCommand implements ReadCommand<SftpFileAttributes> {

  /**
   * {@inheritDoc}
   */
  public SftpReadCommand(SftpFileSystemConnection fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  @Override
  public Result<InputStream, SftpFileAttributes> read(FileConnectorConfig config, String filePath, boolean lock,
                                                      Long timeBetweenSizeCheck) {
    SftpFileAttributes attributes = getExistingFile(filePath);
    if (attributes.isDirectory()) {
      throw cannotReadDirectoryException(createUri(attributes.getPath()));
    }

    return read(config, attributes, lock, timeBetweenSizeCheck, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<InputStream, SftpFileAttributes> read(FileConnectorConfig config, SftpFileAttributes attributes, boolean lock,
                                                      Long timeBetweenSizeCheck) {
    return read(config, attributes, lock, timeBetweenSizeCheck, false);
  }

  public SftpFileAttributes readAttributes(String filePath) {
    return getFile(filePath);
  }

  private Result<InputStream, SftpFileAttributes> read(FileConnectorConfig config, SftpFileAttributes attributes, boolean lock,
                                                       Long timeBetweenSizeCheck, boolean useCurrentConnection) {
    URI uri = UriUtils.createUri(attributes.getPath());

    UriLock pathLock = lock ? fileSystem.lock(uri) : new NullUriLock(uri);
    InputStream payload = null;
    try {
      payload = getFileInputStream((SftpConnector) config, attributes, pathLock, timeBetweenSizeCheck, useCurrentConnection);
      MediaType resolvedMediaType = fileSystem.getFileMessageMediaType(attributes);
      return Result.<InputStream, SftpFileAttributes>builder().output(payload).mediaType(resolvedMediaType).attributes(attributes)
          .build();
    } catch (Exception e) {
      IOUtils.closeQuietly(payload);
      throw client.handleException("Could not fetch file " + uri.getPath(), e);
    } finally {
      pathLock.release();
    }
  }

  private InputStream getFileInputStream(SftpConnector config, SftpFileAttributes attributes, UriLock pathLock,
                                         Long timeBetweenSizeCheck, boolean useCurrentConnection)
      throws ConnectionException {
    if (useCurrentConnection) {
      return SftpInputStream.newInstance(fileSystem, attributes, pathLock, timeBetweenSizeCheck);
    } else {
      return SftpInputStream.newInstance(config, attributes, pathLock, timeBetweenSizeCheck);
    }
  }
}
