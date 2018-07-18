/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.file.common.api.stream.LazyStreamSupplier;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.extension.sftp.internal.exception.DeletedFileWhileReadException;
import org.mule.extension.sftp.internal.exception.FileBeingModifiedException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
   * @param config     the config which is parameterizing this operation
   * @param attributes a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock       the {@link PathLock} to be used
   * @return a new {@link SftpFileAttributes}
   * @throws ConnectionException if a connection could not be established
   */
  public static SftpInputStream newInstance(SftpConnector config, SftpFileAttributes attributes, PathLock lock,
                                            Long timeBetweenSizeCheck, TimeUnit timeBetweenSizeCheckUnit)
      throws ConnectionException {
    ConnectionAwareSupplier connectionAwareSupplier =
        new ConnectionAwareSupplier(attributes, getConnectionManager(config),
                                    calculateTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit),
                                    config);
    return new SftpInputStream(connectionAwareSupplier, lock);
  }

  private static Long calculateTimeBetweenSizeCheckInMillis(Long timeBetweenSizeCheck, TimeUnit timeBetweenSizeCheckUnit) {
    if (timeBetweenSizeCheck == null) {
      return null;
    }
    if (timeBetweenSizeCheck < 1) {
      throw new IllegalArgumentException("timeBetweenSizeCheck must be greater than 1.");
    }
    return timeBetweenSizeCheckUnit.convert(timeBetweenSizeCheck, MILLISECONDS);
  }

  private ConnectionAwareSupplier connectionAwareSupplier;

  protected SftpInputStream(ConnectionAwareSupplier connectionAwareSupplier, PathLock lock) {
    super(new LazyStreamSupplier(connectionAwareSupplier), lock);
    this.connectionAwareSupplier = connectionAwareSupplier;
  }

  @Override
  protected void doClose() throws IOException {
    try {
      super.doClose();
    } finally {
      ConnectionHandler connectionHandler = connectionAwareSupplier.getConnectionHandler();
      if (connectionHandler != null) {
        connectionHandler.release();
      }
    }
  }

  protected static class ConnectionAwareSupplier implements Supplier<InputStream> {

    private static final Logger LOGGER = getLogger(ConnectionAwareSupplier.class);
    private static final String FILE_NO_LONGER_EXISTS_MESSAGE =
        "Error reading file from path %s. It no longer exists at the time of reading.";
    private static final String STARTING_WAIT_MESSAGE = "Starting wait to check if the file size of the file %s is stable.";
    private static final int MAX_SIZE_CHECK_RETRIES = 2;

    private ConnectionHandler<SftpFileSystem> connectionHandler;
    private SftpFileAttributes attributes;
    private ConnectionManager connectionManager;
    private Long timeBetweenSizeCheck;
    private SftpConnector config;

    ConnectionAwareSupplier(SftpFileAttributes attributes, ConnectionManager connectionManager,
                            Long timeBetweenSizeCheck, SftpConnector config) {
      this.attributes = attributes;
      this.connectionManager = connectionManager;
      this.timeBetweenSizeCheck = timeBetweenSizeCheck;
      this.config = config;
    }

    @Override
    public InputStream get() {
      try {
        SftpFileAttributes updatedAttributes = getUpdatedAttributes(config, connectionManager, attributes.getPath());
        if (updatedAttributes != null && timeBetweenSizeCheck != null && timeBetweenSizeCheck > 0) {
          updatedAttributes = getUpdatedStableAttributes(config, connectionManager, updatedAttributes);
        }
        if (updatedAttributes == null) {
          throw new DeletedFileWhileReadException(createStaticMessage("File on path " + attributes.getPath()
              + " was read but does not exist anymore."));
        }
        connectionHandler = connectionManager.getConnection(config);
        return connectionHandler.getConnection().retrieveFileContent(updatedAttributes);
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    }

    private SftpFileAttributes getUpdatedStableAttributes(SftpConnector config, ConnectionManager connectionManager,
                                                          SftpFileAttributes updatedAttributes)
        throws ConnectionException {
      SftpFileAttributes oldAttributes;
      int retries = 0;
      do {
        oldAttributes = updatedAttributes;
        try {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(STARTING_WAIT_MESSAGE, attributes.getPath()));
          }
          sleep(timeBetweenSizeCheck);
        } catch (InterruptedException e) {
          throw new MuleRuntimeException(createStaticMessage("Execution was interrupted while waiting to recheck file sizes"),
                                         e);
        }
        updatedAttributes = getUpdatedAttributes(config, connectionManager, attributes.getPath());
      } while (updatedAttributes != null && updatedAttributes.getSize() != oldAttributes.getSize()
          && retries++ < MAX_SIZE_CHECK_RETRIES);
      if (retries > MAX_SIZE_CHECK_RETRIES) {
        throw new FileBeingModifiedException(createStaticMessage("File on path " + attributes.getPath()
            + " is still being written."));
      }
      return updatedAttributes;
    }

    private SftpFileAttributes getUpdatedAttributes(SftpConnector config, ConnectionManager connectionManager, String filePath)
        throws ConnectionException {
      ConnectionHandler<SftpFileSystem> connectionHandler = connectionManager.getConnection(config);
      SftpFileSystem sftpFileSystem = connectionHandler.getConnection();
      SftpFileAttributes updatedSftpFileAttributes = sftpFileSystem.readFileAttributes(filePath);
      connectionHandler.release();
      if (updatedSftpFileAttributes == null) {
        LOGGER.error(String.format(FILE_NO_LONGER_EXISTS_MESSAGE, filePath));
      }
      return updatedSftpFileAttributes;
    }

    public ConnectionHandler getConnectionHandler() {
      return connectionHandler;
    }

  }
}
