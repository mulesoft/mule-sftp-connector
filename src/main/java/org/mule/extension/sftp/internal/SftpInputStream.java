/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;

import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.mule.extension.file.common.api.AbstractConnectedFileInputStreamSupplier;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.lock.UriLock;
import org.mule.extension.file.common.api.stream.AbstractNonFinalizableFileInputStream;
import org.mule.extension.file.common.api.stream.LazyStreamSupplier;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of {@link AbstractNonFinalizableFileInputStream} for SFTP connections
 *
 * @since 1.0
 */
public class SftpInputStream extends AbstractNonFinalizableFileInputStream {

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
   * @param lock the {@link UriLock} to be used
   * @param timeBetweenSizeCheck time in milliseconds to wait between size checks to decide if a file is ready to be read
   * @return a new {@link SftpFileAttributes}
   * @throws ConnectionException if a connection could not be established
   */
  public static SftpInputStream newInstance(SftpConnector config, SftpFileAttributes attributes, UriLock lock,
                                            Long timeBetweenSizeCheck)
      throws ConnectionException {
    SftpFileInputStreamSupplier sftpFileInputStreamSupplier =
        new SftpFileInputStreamSupplier(attributes, getConnectionManager(config), timeBetweenSizeCheck, config);
    return new SftpInputStream(sftpFileInputStreamSupplier, lock);
  }

  /**
   * Using the given connection ,returns a new instance of this class.
   * <p>
   * Instances returned by this method <b>MUST</b> be closed or fully consumed.
   *
   * @param fileSystem            the {@link SftpFileSystem} to be used to connect to the FTP server
   * @param attributes            a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock                  the {@link UriLock} to be used
   * @param timeBetweenSizeCheck  the time to be waited between size checks if configured.
   * @return a mew {@link SftpInputStream}
   * @return
   */
  public static SftpInputStream newInstance(SftpFileSystem fileSystem, SftpFileAttributes attributes, UriLock lock,
                                            Long timeBetweenSizeCheck) {
    SftpFileInputStreamSupplier sftpFileInputStreamSupplier =
        new SftpFileInputStreamSupplier(attributes, timeBetweenSizeCheck, fileSystem);
    return new SftpInputStream(sftpFileInputStreamSupplier, lock);
  }

  private final SftpFileInputStreamSupplier sftpFileInputStreamSupplier;

  protected SftpInputStream(SftpFileInputStreamSupplier sftpFileInputStreamSupplier, UriLock lock) {
    super(new LazyStreamSupplier(sftpFileInputStreamSupplier), lock);
    this.sftpFileInputStreamSupplier = sftpFileInputStreamSupplier;
  }

  @Override
  protected void doClose() throws IOException {
    try {
      super.doClose();
    } finally {
      sftpFileInputStreamSupplier.releaseConnectionUsedForContentInputStream();
    }
  }

  protected static class SftpFileInputStreamSupplier extends AbstractConnectedFileInputStreamSupplier<SftpFileSystem> {

    private SftpFileInputStreamSupplier(SftpFileAttributes attributes, ConnectionManager connectionManager,
                                        Long timeBetweenSizeCheck, SftpConnector config) {
      super(attributes, connectionManager, timeBetweenSizeCheck, config);
    }

    private SftpFileInputStreamSupplier(SftpFileAttributes attributes, Long timeBetweenSizeCheck, SftpFileSystem fileSystem) {
      super(attributes, timeBetweenSizeCheck, fileSystem);
    }

    @Override
    protected FileAttributes getUpdatedAttributes(SftpFileSystem fileSystem) {
      return fileSystem.readFileAttributes(attributes.getPath());
    }

    @Override
    protected InputStream getContentInputStream(SftpFileSystem fileSystem) {
      return fileSystem.retrieveFileContent(attributes);
    }

    @Override
    protected boolean fileWasDeleted(MuleRuntimeException e) {
      if (e.getCause() instanceof SftpException) {
        return ((SftpException) e.getCause()).getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE;
      }
      return false;
    }
  }
}
