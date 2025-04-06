/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.api.connection.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SmallTest
public class SftpConnectionProviderTestCase {

  private SftpConnectionProvider sftpConnectionProvider = new SftpConnectionProvider();
  private SftpConnectionProvider sftpConnectionProvider2 = new SftpConnectionProvider();
  private SftpProxyConfig proxyConfig = new SftpProxyConfig();

  @Test
  public void testSftpConnectionProvider() {

    sftpConnectionProvider.setProxyConfig(proxyConfig);
    sftpConnectionProvider2.setProxyConfig(proxyConfig);
    assertEquals(sftpConnectionProvider, sftpConnectionProvider2);

    Object other = new Object();
    assertNotEquals(sftpConnectionProvider, other);
    sftpConnectionProvider.equals(sftpConnectionProvider);

    sftpConnectionProvider.hashCode();
  }

  @Test(expected = NullPointerException.class)
  public void testSftpConnectionProviderForNullPointer() throws ConnectionException {
    sftpConnectionProvider.connect();
  }
}
