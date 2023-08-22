/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lifecycle;

import static org.mule.test.extension.file.common.api.FileTestHarness.WORKING_DIR_SYSTEM_PROPERTY;

import org.mule.extension.sftp.SftpServer;

import java.io.IOException;

import org.junit.rules.TemporaryFolder;

public class SftpServerLifecycleManager {


  private static TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static SftpServer sftpServer;

  public enum AuthType {
    USER_PASSWORD, PUBLIC_KEY
  }

  public static void startSftpServer(String port) throws Exception {
    createAndStartServer(Integer.valueOf(port));
  }

  private static void createAndStartServer(Integer port) throws IOException {
    temporaryFolder.create();
    setUpServer(port);
  }

  private static void setUpServer(int port) {
    sftpServer = new SftpServer(port, temporaryFolder.getRoot().toPath());
    sftpServer.setPasswordAuthenticator();
    sftpServer.start();
  }

  public static void stopSftpServer() throws Exception {
    try {
      if (sftpServer != null) {
        sftpServer.stop();
      }
    } finally {
      temporaryFolder.delete();
    }
  }

}
