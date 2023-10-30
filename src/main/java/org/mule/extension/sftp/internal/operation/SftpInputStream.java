/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.extension.sftp.internal.lock.UriLock;
import org.mule.extension.sftp.internal.stream.AbstractNonFinalizableFileInputStream;
import org.mule.extension.sftp.internal.stream.LazyStreamSupplier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;

/**
 * Implementation of {@link AbstractNonFinalizableFileInputStream} for SFTP connections
 *
 * @since 1.0
 */
public class SftpInputStream extends AbstractNonFinalizableFileInputStream {


  /**
   * Using the given connection ,returns a new instance of this class.
   * <p>
   * Instances returned by this method <b>MUST</b> be closed or fully consumed.
   *
   * @param fileSystem           the {@link SftpFileSystemConnection} to be used to connect to the FTP server
   * @param attributes           a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock                 the {@link UriLock} to be used
   * @param timeBetweenSizeCheck the time to be waited between size checks if configured.
   * @return a mew {@link SftpInputStream}
   * @return
   */
  public static SftpInputStream newInstance(SftpFileSystemConnection fileSystem, SftpFileAttributes attributes, UriLock lock,
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

  protected static class SftpFileInputStreamSupplier extends AbstractConnectedFileInputStreamSupplier<SftpFileSystemConnection> {

    private SftpFileInputStreamSupplier(SftpFileAttributes attributes, Long timeBetweenSizeCheck,
                                        SftpFileSystemConnection fileSystem) {
      super(attributes, timeBetweenSizeCheck, fileSystem);
    }

    @Override
    protected FileAttributes getUpdatedAttributes(SftpFileSystemConnection fileSystem) {
      return fileSystem.readFileAttributes(attributes.getPath());
    }

    @Override
    protected InputStream getContentInputStream(SftpFileSystemConnection fileSystem) {
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
