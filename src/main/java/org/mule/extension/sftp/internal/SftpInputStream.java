/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.AbstractFileInputStreamSupplier;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.file.common.api.stream.LazyStreamSupplier;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;

import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * Implementation of {@link AbstractFileInputStream} for SFTP connections
 *
 * @since 1.0
 */
public class SftpInputStream extends AbstractFileInputStream {

  protected static ConnectionManager getConnectionManager(SftpConnector config) throws ConnectionException {
    return config.getConnectionManager();
  }

  /**
   * Establishes the underlying connection and returns a new instance of this class.
   * <p>
   * Instances returned by this method <b>MUST</b> be closed or fully consumed.
   *
   * @param config the config which is parameterizing this operation
   * @param attributes a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock the {@link PathLock} to be used
   * @param timeBetweenSizeCheck time in milliseconds to wait between size checks to decide if a file is ready to be read
   * @return a new {@link SftpFileAttributes}
   * @throws ConnectionException if a connection could not be established
   */
  public static SftpInputStream newInstance(SftpConnector config, SftpFileAttributes attributes, PathLock lock,
                                            Long timeBetweenSizeCheck)
      throws ConnectionException {
    SftpFileInputStreamSupplier sftpFileInputStreamSupplier =
        new SftpFileInputStreamSupplier(attributes, getConnectionManager(config), timeBetweenSizeCheck, config);
    return new SftpInputStream(sftpFileInputStreamSupplier, lock);
  }

  private SftpFileInputStreamSupplier sftpFileInputStreamSupplier;

  protected SftpInputStream(SftpFileInputStreamSupplier sftpFileInputStreamSupplier, PathLock lock) {
    super(new LazyStreamSupplier(sftpFileInputStreamSupplier), lock);
    this.sftpFileInputStreamSupplier = sftpFileInputStreamSupplier;
  }

  @Override
  protected void doClose() throws IOException {
    try {
      super.doClose();
    } finally {
      sftpFileInputStreamSupplier.getConnectionHandler().ifPresent(ConnectionHandler::release);
    }
  }

  protected static class SftpFileInputStreamSupplier extends AbstractFileInputStreamSupplier {

    private static final Logger LOGGER = getLogger(SftpFileInputStreamSupplier.class);
    private static final String FILE_NOT_FOUND_EXCEPTION = "FileNotFoundException";

    private ConnectionHandler<SftpFileSystem> connectionHandler;
    private ConnectionManager connectionManager;
    private SftpFileSystem sftpFileSystem;
    private SftpConnector config;

    SftpFileInputStreamSupplier(SftpFileAttributes attributes, ConnectionManager connectionManager,
                                Long timeBetweenSizeCheck, SftpConnector config) {
      super(attributes, timeBetweenSizeCheck);
      this.connectionManager = connectionManager;
      this.config = config;
    }

    @Override
    protected FileAttributes getUpdatedAttributes() {
      try {
        ConnectionHandler<SftpFileSystem> connectionHandler = connectionManager.getConnection(config);
        SftpFileSystem sftpFileSystem = connectionHandler.getConnection();
        SftpFileAttributes updatedSftpFileAttributes = sftpFileSystem.readFileAttributes(attributes.getPath());
        connectionHandler.release();
        if (updatedSftpFileAttributes == null) {
          LOGGER.error(String.format(FILE_NO_LONGER_EXISTS_MESSAGE, attributes.getPath()));
        }
        return updatedSftpFileAttributes;
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    }

    @Override
    protected InputStream getContentInputStream() {
      try {
        connectionHandler = connectionManager.getConnection(config);
        sftpFileSystem = connectionHandler.getConnection();
        return sftpFileSystem.retrieveFileContent(attributes);
      } catch (MuleRuntimeException e) {
        if (fileWasDeleted(e)) {
          onFileDeleted(e);
        }
        throw e;
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    }

    private boolean fileWasDeleted(Exception e) {
      return e.getCause() instanceof SftpException && e.getCause().getMessage().contains(FILE_NOT_FOUND_EXCEPTION);
    }

    public Optional<ConnectionHandler> getConnectionHandler() {
      return ofNullable(connectionHandler);
    }

  }
}
