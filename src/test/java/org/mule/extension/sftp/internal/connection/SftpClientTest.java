/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.common.SshException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpClientTest {

  private static SftpClient client;

  @BeforeAll
  static void setup() {
    client = mock(SftpClient.class);
  }

  @Test
  void testSftpConnectionException() throws ConnectionException {
    doCallRealMethod().when(client).setProxyConfig(any());
    assertThrows(SftpConnectionException.class, () -> client.setProxyConfig(new SftpProxyConfig()));
  }

  @Test
  void testSftpClientGetAttributesNull() throws ConnectionException, IOException {
    doCallRealMethod().when(client).getAttributes(any());
    assertNull(client.getAttributes(null));
  }

  @Test
  void testSftpClientConfigureHostChecking() throws GeneralSecurityException, IOException {
    SftpClient client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
    client.setKnownHostsFile("HostFile");
    assertThrows(SshException.class, () -> client.login("user"));
  }

  @Test
  void testSftpClientCheckExists() throws GeneralSecurityException, IOException {
    doCallRealMethod().when(client).setIdentity(anyString(), anyString());
    assertThrows(IllegalArgumentException.class, () -> client.setIdentity("HostFile", "passphrase"));
  }

  @Test
  void testSftpClientDisconnect() {
    SftpClient spyClient = spy(new SftpClient("host", 80, PRNGAlgorithm.SHA1PRNG, null));
    SftpFileSystemConnection fileSystemConnection = new SftpFileSystemConnection(spyClient, "", null);
    fileSystemConnection.disconnect();
    verify(spyClient, times(1)).disconnect();
  }

  @Test
  void testSftpClientGetFile() throws URISyntaxException {
    SftpClient client = new SftpClient("host", 80, PRNGAlgorithm.SHA1PRNG, null);
    URI uri = new URI("path");
    assertThrows(MuleRuntimeException.class, () -> client.getFile(uri));
  }

}
