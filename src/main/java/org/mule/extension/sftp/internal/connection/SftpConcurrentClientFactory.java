/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import java.io.IOException;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpErrorDataHandler;
import org.apache.sshd.sftp.client.SftpVersionSelector;
import org.apache.sshd.sftp.client.impl.DefaultSftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;

public class SftpConcurrentClientFactory extends DefaultSftpClientFactory {

  static SftpConcurrentClientFactory instance() {
    return SftpConcurrentClientFactory.INSTANCE;
  }

  public static final SftpConcurrentClientFactory INSTANCE = new SftpConcurrentClientFactory();

  public SftpConcurrentClientFactory() {
    super();
  }

  @Override
  protected DefaultSftpClient createDefaultSftpClient(
                                                      ClientSession session, SftpVersionSelector selector,
                                                      SftpErrorDataHandler errorDataHandler)
      throws IOException {
    return new SftpConcurrentClient(session, selector, errorDataHandler);
  }
}
