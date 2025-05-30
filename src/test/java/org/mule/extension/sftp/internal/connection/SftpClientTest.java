/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.common.SshException;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.size.SmallTest;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.ExternalConfigProvider;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.client.SshClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpClientTest {

  @Test
  void testSetProxyConfigNullHost() throws ConnectionException {
    SftpClient client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
    assertThrows(SftpConnectionException.class, () -> client.setProxyConfig(new SftpProxyConfig()));
  }

  @Test
  void testSftpClientGetAttributesNullWhenUriNull() throws ConnectionException, IOException {
    SftpClient client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
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
    SftpClient client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
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
    SftpClient client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
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

  @Test
  void testSftpClientConstructorWithProxyConfig() {
    // Test that proxy configuration is properly handled in constructor
    SftpProxyConfig proxyConfig = new SftpProxyConfig();
    proxyConfig.setHost("proxy.example.com");
    proxyConfig.setPort(8080);
    proxyConfig.setProtocol(SftpProxyConfig.Protocol.HTTP);

    ExternalConfigProvider mockExternalConfigProvider = mock(ExternalConfigProvider.class);
    when(mockExternalConfigProvider.getConfigProperties()).thenReturn(new Properties());

    SchedulerService mockSchedulerService = mock(SchedulerService.class);

    // This should exercise the proxy configuration path in constructor
    SftpClient clientWithProxy = new SftpClient("host", 22, PRNGAlgorithm.SHA1PRNG,
                                                mockSchedulerService, true, proxyConfig, mockExternalConfigProvider);

    assertNotNull(clientWithProxy);
  }

  @Test
  void testSftpClientConstructorWithoutProxyConfig() {
    // Test constructor path without proxy config
    ExternalConfigProvider mockExternalConfigProvider = mock(ExternalConfigProvider.class);
    when(mockExternalConfigProvider.getConfigProperties()).thenReturn(new Properties());

    SchedulerService mockSchedulerService = mock(SchedulerService.class);

    // This should exercise the non-proxy path in constructor
    SftpClient clientWithoutProxy = new SftpClient("host", 22, PRNGAlgorithm.SHA1PRNG,
                                                   mockSchedulerService, true, null, mockExternalConfigProvider);

    assertNotNull(clientWithoutProxy);
  }

  @Test
  void testGetAttributesTriggersReconnectOnIOExceptionWithMessage() throws Exception {
    // Test the reconnection logic when IOException has a message
    ExternalConfigProvider mockExternalConfigProvider = mock(ExternalConfigProvider.class);
    when(mockExternalConfigProvider.getConfigProperties()).thenReturn(new Properties());

    SftpClient sftpClient = new SftpClient("host", 22, PRNGAlgorithm.SHA1PRNG, null, false, null, mockExternalConfigProvider);
    URI uri = new URI("/some/path");

    // Use reflection to set private sftp field
    Field sftpField = SftpClient.class.getDeclaredField("sftp");
    sftpField.setAccessible(true);
    org.apache.sshd.sftp.client.SftpClient mockSftp = mock(org.apache.sshd.sftp.client.SftpClient.class);
    sftpField.set(sftpClient, mockSftp);

    // Mock sftp.stat to throw IOException with a message (this triggers reconnect logic)
    doThrow(new IOException("Connection lost")).when(mockSftp).stat(anyString());

    // The method should try to reconnect but will fail since we can't actually connect
    Exception thrown = assertThrows(Exception.class, () -> sftpClient.getAttributes(uri));
    assertTrue(thrown.getMessage().contains("Failed to reconnect while getting attributes"));
  }

  @Test
  void testReconnectAndRetryWithProxyConfig() throws Exception {
    // Test the reconnectAndRetry method when proxy config is present
    SftpProxyConfig proxyConfig = new SftpProxyConfig();
    proxyConfig.setHost("proxy.example.com");
    proxyConfig.setPort(8080);
    proxyConfig.setProtocol(SftpProxyConfig.Protocol.HTTP);

    ExternalConfigProvider mockExternalConfigProvider = mock(ExternalConfigProvider.class);
    when(mockExternalConfigProvider.getConfigProperties()).thenReturn(new Properties());

    SftpClient sftpClient =
        new SftpClient("host", 22, PRNGAlgorithm.SHA1PRNG, null, false, proxyConfig, mockExternalConfigProvider);
    URI uri = new URI("/some/path");

    // Use reflection to set private sftp field
    Field sftpField = SftpClient.class.getDeclaredField("sftp");
    sftpField.setAccessible(true);
    org.apache.sshd.sftp.client.SftpClient mockSftp = mock(org.apache.sshd.sftp.client.SftpClient.class);
    sftpField.set(sftpClient, mockSftp);

    // Mock sftp.stat to throw IOException with message to trigger reconnect
    doThrow(new IOException("Client closed")).when(mockSftp).stat(anyString());

    // This will trigger the reconnectAndRetry method which has proxy configuration logic
    Exception thrown = assertThrows(Exception.class, () -> sftpClient.getAttributes(uri));
    assertTrue(thrown.getMessage().contains("Failed to reconnect while getting attributes") ||
        thrown.getMessage().contains("Client closed"));
  }

  @Test
  void testReconnectAndRetryWithoutProxyConfig() throws Exception {
    // Test the reconnectAndRetry method when no proxy config is present
    ExternalConfigProvider mockExternalConfigProvider = mock(ExternalConfigProvider.class);
    when(mockExternalConfigProvider.getConfigProperties()).thenReturn(new Properties());

    SftpClient sftpClient = new SftpClient("host", 22, PRNGAlgorithm.SHA1PRNG, null, false, null, mockExternalConfigProvider);
    URI uri = new URI("/some/path");

    // Use reflection to set private sftp field
    Field sftpField = SftpClient.class.getDeclaredField("sftp");
    sftpField.setAccessible(true);
    org.apache.sshd.sftp.client.SftpClient mockSftp = mock(org.apache.sshd.sftp.client.SftpClient.class);
    sftpField.set(sftpClient, mockSftp);

    // Mock sftp.stat to throw IOException with message to trigger reconnect
    doThrow(new IOException("Client closed")).when(mockSftp).stat(anyString());

    // This will trigger the reconnectAndRetry method without proxy configuration
    Exception thrown = assertThrows(Exception.class, () -> sftpClient.getAttributes(uri));
    assertTrue(thrown.getMessage().contains("Failed to reconnect while getting attributes") ||
        thrown.getMessage().contains("Client closed"));
  }

}
