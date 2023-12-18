/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.sftp.client.SftpErrorDataHandler;
import org.apache.sshd.sftp.client.SftpVersionSelector;
import org.apache.sshd.sftp.client.impl.DefaultSftpClient;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SftpConcurrentClient extends DefaultSftpClient {

  /**
   * @param clientSession          The {@link ClientSession}
   * @param initialVersionSelector The initial {@link SftpVersionSelector} - if {@code null} then version 6 is
   *                               assumed.
   * @param errorDataHandler       The {@link SftpErrorDataHandler} to handle incoming data through the error stream
   *                               - if {@code null} the data is silently ignored
   * @throws IOException If failed to initialize
   */
  private final Lock sendLock = new ReentrantLock();

  public SftpConcurrentClient(ClientSession clientSession, SftpVersionSelector initialVersionSelector,
                              SftpErrorDataHandler errorDataHandler)
      throws IOException {
    super(clientSession, initialVersionSelector, errorDataHandler);
  }

  @Override
  public int send(int cmd, Buffer buffer) throws IOException {
    this.sendLock.lock();
    try {
      return super.send(cmd, buffer);
    } finally {
      this.sendLock.unlock();
    }
  }
}
