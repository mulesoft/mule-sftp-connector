/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.sftp.client.SftpErrorDataHandler;
import org.apache.sshd.sftp.client.SftpVersionSelector;
import org.apache.sshd.sftp.client.impl.DefaultSftpClient;


/**
 * This class was extended from {@link DefaultSftpClient} to be able to override the {@link DefaultSftpClient#send(int, Buffer)} method
 * and thus synchronize the use of the buffer provided by ApacheMina.
 * More information GUS: W-14203139
 */
public class SftpConcurrentClient extends DefaultSftpClient {

  private final Lock sendLock = new ReentrantLock();

  /**
   * @param clientSession          The {@link ClientSession}
   * @param initialVersionSelector The initial {@link SftpVersionSelector} - if {@code null} then version 6 is
   *                               assumed.
   * @param errorDataHandler       The {@link SftpErrorDataHandler} to handle incoming data through the error stream
   *                               - if {@code null} the data is silently ignored
   * @throws IOException If failed to initialize
   */
  public SftpConcurrentClient(ClientSession clientSession, SftpVersionSelector initialVersionSelector,
                              SftpErrorDataHandler errorDataHandler)
      throws IOException {
    super(clientSession, initialVersionSelector, errorDataHandler);
  }

  /**
   * TODO: The solution of synchronizing the send operation can cause a bottleneck
   * when you have multiple files waiting to be processed by this operation, producing a degradation in processing.
   * For the moment it is enough to be able to escape the error when processing multiple files,
   * but we must find a way so that each thread of execution can manage its own buffer (it is just a suggestion)
   * or another way in which we do not have a bottleneck here .
   */
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
