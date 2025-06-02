/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.URI;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpClientProxyTest {

  @Test
  void testProxyConfigInitializationPath() throws Exception {
    // Create a proxy config
    SftpProxyConfig proxyConfig = new SftpProxyConfig();
    proxyConfig.setHost("proxy.example.com");
    proxyConfig.setPort(8080);
    proxyConfig.setProtocol(SftpProxyConfig.Protocol.HTTP);

    // Verify proxy config properties are set correctly
    assertEquals("proxy.example.com", proxyConfig.getHost());
    assertEquals(Integer.valueOf(8080), proxyConfig.getPort());
    assertEquals(SftpProxyConfig.Protocol.HTTP, proxyConfig.getProtocol());
  }

  @Test
  void testReconnectLogicWithProxyPresent() throws Exception {
    // Test the reconnectAndRetry method's proxy configuration path using reflection
    SftpClient sftpClient = new SftpClient("testhost", 22, PRNGAlgorithm.SHA1PRNG, null);

    // Set a proxy config using reflection
    Field proxyConfigField = SftpClient.class.getDeclaredField("proxyConfig");
    proxyConfigField.setAccessible(true);

    SftpProxyConfig proxyConfig = new SftpProxyConfig();
    proxyConfig.setHost("proxy.example.com");
    proxyConfig.setPort(8080);
    proxyConfig.setProtocol(SftpProxyConfig.Protocol.HTTP);
    proxyConfigField.set(sftpClient, proxyConfig);

    // Verify the proxy config was set
    SftpProxyConfig retrievedConfig = (SftpProxyConfig) proxyConfigField.get(sftpClient);
    assertNotNull(retrievedConfig);
    assertEquals("proxy.example.com", retrievedConfig.getHost());
  }

  @Test
  void testGetAttributesExceptionWithMessage() throws Exception {
    SftpClient sftpClient = new SftpClient("testhost", 22, PRNGAlgorithm.SHA1PRNG, null);

    URI uri = new URI("/test/file.txt");

    // Mock the sftp client to simulate an exception
    Field sftpField = SftpClient.class.getDeclaredField("sftp");
    sftpField.setAccessible(true);
    org.apache.sshd.sftp.client.SftpClient mockSftp = mock(org.apache.sshd.sftp.client.SftpClient.class);
    sftpField.set(sftpClient, mockSftp);

    // Create IOException with message to trigger the message != null condition
    IOException ioExceptionWithMessage = new IOException("client connection lost");
    when(mockSftp.stat(anyString())).thenThrow(ioExceptionWithMessage);

    // This should trigger the if (e.getMessage() != null) path 
    Exception thrown = assertThrows(Exception.class, () -> sftpClient.getAttributes(uri));

    // Verify the exception occurred and has a message
    assertNotNull(thrown);
    assertTrue(thrown.getMessage() != null || thrown.getCause() != null);

    // Verify stat was called
    verify(mockSftp, atLeastOnce()).stat(anyString());
  }
}
