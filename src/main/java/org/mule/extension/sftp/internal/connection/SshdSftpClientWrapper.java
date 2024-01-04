/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.sftp.client.SftpClient;

public class SshdSftpClientWrapper {

  private org.apache.sshd.sftp.client.SftpClient client;

  public void setClient(SftpClient client) {
    this.client = client;
  }

  public SftpClient getClient() {
    return client;
  }
}
