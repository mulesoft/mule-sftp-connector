/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lifecycle;

import org.junit.rules.TemporaryFolder;
import org.mule.extension.sftp.TestProxyServer;

public class ProxyServerLifecycleManager {


  private static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static TestProxyServer proxyServer;

  public static void startProxyServer(String proxyPort, String serverPort) throws Exception {
    createAndStartServer(Integer.valueOf(proxyPort), Integer.valueOf(serverPort));
  }

  private static void createAndStartServer(int proxyPort, int serverPort) throws Exception {
    proxyServer = new TestProxyServer(proxyPort, serverPort);
    proxyServer.start();
  }

  public static void stopProxyServer() throws Exception {
    try {
      if (proxyServer != null) {
        proxyServer.stop();
      }
    } finally {
      temporaryFolder.delete();
    }
  }

}
