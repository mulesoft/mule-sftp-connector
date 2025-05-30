/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.tck.size.SmallTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SmallTest
public class MuleSftpClientTest {

  static MuleSftpClient client;

  @BeforeAll
  static void setup() {
    client = new MuleSftpClient();
  }

  @Test
  void testGetProxyConfig() {
    client.setProxyConfig(new SftpProxyConfig());
    assertNotNull(client.getProxyConfig());
  }

  @Test
  void testDoConnectWithNullConnector() {
    assertThrows(IllegalStateException.class, () -> client.doConnect("user", null, null, null, null, null));
  }

}
