/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.connection.SftpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class SftpCustomWriter implements SftpWriter {

  private SftpClient muleSftpClient;
  private org.apache.sshd.sftp.client.SftpClient apacheSftpClient;
  private CustomWriteBufferSize bufferSizeForWriteStrategy;

  public SftpCustomWriter(org.mule.extension.sftp.internal.connection.SftpClient muleSftpClient,
                          org.apache.sshd.sftp.client.SftpClient apacheSftpClient,
                          CustomWriteBufferSize bufferSizeForWriteStrategy) {
    this.muleSftpClient = muleSftpClient;
    this.apacheSftpClient = apacheSftpClient;
    this.bufferSizeForWriteStrategy = bufferSizeForWriteStrategy;
  }

  @Override
  public void write(String path, InputStream stream, FileWriteMode mode, URI uri) throws IOException {
    try (org.apache.sshd.sftp.client.SftpClient.CloseableHandle handle =
        muleSftpClient.open(path, mode)) {
      byte[] buf = new byte[bufferSizeForWriteStrategy.getCustomWriteBufferSize()];
      FileAttributes file = muleSftpClient.getFile(uri);
      long offSet = file != null ? file.getSize() : 0;
      int n;
      while ((n = stream.read(buf)) != -1) {
        apacheSftpClient.write(handle, offSet, buf, 0, n);
        offSet += n;
      }
    }

  }
}
