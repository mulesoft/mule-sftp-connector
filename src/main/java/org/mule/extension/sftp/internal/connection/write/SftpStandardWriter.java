/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.connection.SftpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import static org.mule.extension.sftp.api.CustomWriteBufferSize.BUFFER_SIZE_8KB;

/**
 * A {@link SftpWriter} contract
 *
 * @since 2.3
 */
public class SftpStandardWriter implements SftpWriter {

  private SftpClient sftpClient;

  public SftpStandardWriter(SftpClient sftpClient) {
    this.sftpClient = sftpClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String path, InputStream stream, FileWriteMode mode, URI uri) throws IOException {
    try (OutputStream out = sftpClient.getOutputStream(path, mode)) {
      byte[] buf = new byte[BUFFER_SIZE_8KB.getCustomWriteBufferSize()];
      int n;
      while ((n = stream.read(buf)) != -1) {
        out.write(buf, 0, n);
      }
    }

  }
}
