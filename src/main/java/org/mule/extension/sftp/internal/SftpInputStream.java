/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.file.common.api.stream.LazyStreamSupplier;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Implementation of {@link AbstractFileInputStream} for SFTP connections
 *
 * @since 1.0
 */
public class SftpInputStream extends AbstractFileInputStream {

  protected static ConnectionHandler<SftpFileSystem> getConnectionHandler(SftpConnector config) throws ConnectionException {
    return config.getConnectionManager().getConnection(config);
  }

  protected static Supplier<InputStream> getStreamSupplier(SftpFileAttributes attributes,
                                                           ConnectionHandler<SftpFileSystem> connectionHandler) {
    Supplier<InputStream> streamSupplier = () -> {
      try {
        return connectionHandler.getConnection().retrieveFileContent(attributes);
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    };

    return streamSupplier;
  }

  /**
   * Establishes the underlying connection and returns a new instance of this class.
   * <p>
   * Instances returned by this method <b>MUST</b> be closed or fully consumed.
   *
   * @param config     the config which is parameterizing this operation
   * @param attributes a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock       the {@link PathLock} to be used
   * @return a new {@link SftpFileAttributes}
   * @throws ConnectionException if a connection could not be established
   */
  public static SftpInputStream newInstance(SftpConnector config, SftpFileAttributes attributes, PathLock lock)
      throws ConnectionException {
    ConnectionHandler<SftpFileSystem> connectionHandler = getConnectionHandler(config);
    return new SftpInputStream(getStreamSupplier(attributes, connectionHandler), connectionHandler, lock);
  }

  private final SftpFileSystem ftpFileSystem;
  private final ConnectionHandler<SftpFileSystem> connectionHandler;

  private SftpInputStream(Supplier<InputStream> streamSupplier, ConnectionHandler<SftpFileSystem> connectionHandler,
                          PathLock lock)
      throws ConnectionException {
    super(new LazyStreamSupplier(streamSupplier), lock);
    this.connectionHandler = connectionHandler;
    this.ftpFileSystem = connectionHandler.getConnection();
  }

  @Override
  protected void doClose() throws IOException {
    try {
      super.doClose();
    } finally {
      connectionHandler.release();
    }
  }

  /**
   * @return the {@link SftpFileSystem} used to obtain the stream
   */
  protected SftpFileSystem getFtpFileSystem() {
    return ftpFileSystem;
  }
}
