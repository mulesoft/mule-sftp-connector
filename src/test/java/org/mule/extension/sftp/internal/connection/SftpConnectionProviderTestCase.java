/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.api.connection.ConnectionException;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SmallTest
public class SftpConnectionProviderTestCase {

  private SftpConnectionProvider sftpConnectionProvider;
  private SftpConnectionProvider sftpConnectionProvider2;
  private SftpProxyConfig proxyConfig;

  @BeforeEach
  public void setup() {
    sftpConnectionProvider = new SftpConnectionProvider();
    sftpConnectionProvider2 = new SftpConnectionProvider();
    proxyConfig = new SftpProxyConfig();

    sftpConnectionProvider.setProxyConfig(proxyConfig);
    sftpConnectionProvider2.setProxyConfig(proxyConfig);
  }

  @Test
  public void testSftpConnectionProviderEquals() {
    assertEquals(sftpConnectionProvider, sftpConnectionProvider2);
  }

  @Test
  public void testSftpConnectionProviderEqualsWithNull() {
    assertNotEquals(sftpConnectionProvider, null);
  }

  @Test
  public void testSftpConnectionProviderNotEquals() {
    TimeoutSettings otherObj = new TimeoutSettings();
    assertNotEquals(sftpConnectionProvider, otherObj);
  }

  @Test
  public void testSftpConnectionProviderHashCode() {
    assertEquals(sftpConnectionProvider.hashCode(), sftpConnectionProvider2.hashCode());
  }
}
