/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.extension.sftp.internal.lock.URLPathLock;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpClientTest {

  private SftpClient client = mock(SftpClient.class);

  @Test
  public void testSftpConnectionException() throws ConnectionException {
    doCallRealMethod().when(client).setProxyConfig(any());
    assertThrows(SftpConnectionException.class, () -> client.setProxyConfig(new SftpProxyConfig()));
  }

  @Test
  public void testSftpClientGetAttributesNull() throws ConnectionException, IOException {
    doCallRealMethod().when(client).getAttributes(any());
    assertNull(client.getAttributes(null));
  }

  @Test
  public void testSftpClientGetProxyConfigUsernameNull() throws ConnectionException, IOException {
    SftpProxyConfig proxyConfig = new SftpProxyConfig();
    proxyConfig.setHost("localhost");
    proxyConfig.setPort(8081);
    proxyConfig.setUsername("user");
    doCallRealMethod().when(client).setProxyConfig(any());
    assertThrows(SftpConnectionException.class, () -> client.setProxyConfig(proxyConfig));

    URLPathLock urlPathLock = new URLPathLock(null, null);
    urlPathLock.isLocked();
  }

}
