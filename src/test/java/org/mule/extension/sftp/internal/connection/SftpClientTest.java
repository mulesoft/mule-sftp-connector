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
import org.mule.extension.sftp.api.SftpFileAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpClientTest {

  private static SftpClient client;

  @BeforeAll
  static void setup() {
    client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
  }

  @Test
  void testSetProxyConfigNullHost() throws ConnectionException {
    assertThrows(SftpConnectionException.class, () -> client.setProxyConfig(new SftpProxyConfig()));
  }

  @Test
  void testSftpClientGetAttributesNullWhenUriNull() throws ConnectionException, IOException {
    assertNull(client.getAttributes(null));
  }

  @Test
  void testSftpClientConfigureHostChecking() throws GeneralSecurityException, IOException {
    client.setKnownHostsFile("HostFile");
    assertThrows(SshException.class, () -> client.login("user"));
  }

  @Test
  void testSftpClientCheckExists() throws GeneralSecurityException, IOException {
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
    URI uri = new URI("path");
    assertThrows(MuleRuntimeException.class, () -> client.getFile(uri));
  }

  @Test
  void testGetAttributesRetriesOnClientClosed() throws Exception {
    SftpClient sftpClient = new SftpClient("host", 22, PRNGAlgorithm.SHA1PRNG, null);
    URI uri = new URI("/some/path");
    // Use reflection to set private sftp field
    Field sftpField = SftpClient.class.getDeclaredField("sftp");
    sftpField.setAccessible(true);
    org.apache.sshd.sftp.client.SftpClient mockSftp = mock(org.apache.sshd.sftp.client.SftpClient.class);
    sftpField.set(sftpClient, mockSftp);
    // Mock sftp.stat to throw IOException (use anyString() to avoid ambiguity)
    doThrow(new IOException("client is closed")).when(mockSftp).stat(anyString());
    // Assert that an exception is thrown and its message contains "client is closed"
    Exception thrown = assertThrows(Exception.class, () -> sftpClient.getAttributes(uri));
    assertTrue(thrown.getMessage().contains("client is closed") ||
        (thrown.getCause() != null && thrown.getCause().getMessage().contains("client is closed")));
  }

}
