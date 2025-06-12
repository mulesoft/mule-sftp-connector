/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.connection.MuleSftpClientSession;
import org.mule.extension.sftp.internal.exception.ProxyConnectionException;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.buffer.Buffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SmallTest
public class HttpClientConnectorTest {

  private HttpClientConnector connector;
  private MuleSftpClientSession mockClientSession;
  private IoSession mockIoSession;
  private IoWriteFuture mockWriteFuture;
  private InetSocketAddress proxyAddress;
  private InetSocketAddress remoteAddress;

  @BeforeEach
  void setUp() throws IOException {
    proxyAddress = new InetSocketAddress("proxy.example.com", 8080);
    remoteAddress = new InetSocketAddress("target.example.com", 22);
    connector = new HttpClientConnector(proxyAddress, remoteAddress);

    mockClientSession = mock(MuleSftpClientSession.class);
    mockIoSession = mock(IoSession.class);
    mockWriteFuture = mock(IoWriteFuture.class);

    // Set up the mock chain: ClientSession -> IoSession -> WriteBuffer -> WriteFuture
    when(mockClientSession.getIoSession()).thenReturn(mockIoSession);
    when(mockIoSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
  }

  @Test
  void testSendMethodCatchBlockWithIOException() throws Exception {
    // Given: Mock the writeBuffer().verify() to throw IOException
    IOException originalException = new IOException("Network connection failed");
    when(mockWriteFuture.verify(anyLong())).thenThrow(originalException);

    // When & Then: Call sendClientProxyMetadata which internally calls send method
    ProxyConnectionException exception = assertThrows(ProxyConnectionException.class, () -> {
      connector.sendClientProxyMetadata(mockClientSession);
    });

    assertEquals("Failed to send data through proxy", exception.getMessage());
    assertEquals(originalException, exception.getCause());
  }

  @Test
  void testSendMethodCatchBlockWithRuntimeException() throws Exception {
    // Given: Mock the writeBuffer().verify() to throw RuntimeException
    RuntimeException originalException = new RuntimeException("Unexpected error during write operation");
    when(mockWriteFuture.verify(anyLong())).thenThrow(originalException);

    // When & Then: Call sendClientProxyMetadata which internally calls send method
    ProxyConnectionException exception = assertThrows(ProxyConnectionException.class, () -> {
      connector.sendClientProxyMetadata(mockClientSession);
    });

    assertEquals("Failed to send data through proxy", exception.getMessage());
    assertEquals(originalException, exception.getCause());
  }

  @Test
  void testSendMethodCatchBlockWithTimeoutRuntimeException() throws Exception {
    // Given: Mock the writeBuffer().verify() to throw a timeout exception
    RuntimeException timeoutException = new RuntimeException("Connection timeout");
    when(mockWriteFuture.verify(anyLong())).thenThrow(timeoutException);

    // When & Then: Call sendClientProxyMetadata which internally calls send method
    ProxyConnectionException exception = assertThrows(ProxyConnectionException.class, () -> {
      connector.sendClientProxyMetadata(mockClientSession);
    });

    assertEquals("Failed to send data through proxy", exception.getMessage());
    assertEquals(timeoutException, exception.getCause());
  }

  @Test
  void testSendMethodCatchBlockWithNullPointerException() throws Exception {
    // Given: Mock the writeBuffer().verify() to throw NullPointerException
    NullPointerException nullPointerException = new NullPointerException("Null session state");
    when(mockWriteFuture.verify(anyLong())).thenThrow(nullPointerException);

    // When & Then: Call sendClientProxyMetadata which internally calls send method
    ProxyConnectionException exception = assertThrows(ProxyConnectionException.class, () -> {
      connector.sendClientProxyMetadata(mockClientSession);
    });

    assertEquals("Failed to send data through proxy", exception.getMessage());
    assertEquals(nullPointerException, exception.getCause());
  }

  @Test
  void testSendMethodSuccessfulPath() throws Exception {
    // Given: Mock successful writeBuffer and verify operations
    when(mockWriteFuture.verify(anyLong())).thenReturn(mockWriteFuture); // Successful operation

    // When & Then: Call sendClientProxyMetadata and verify no exception is thrown
    assertDoesNotThrow(() -> {
      connector.sendClientProxyMetadata(mockClientSession);
    });

    // Verify the interactions
    verify(mockIoSession).writeBuffer(any(Buffer.class));
    verify(mockWriteFuture).verify(anyLong());
  }

  @Test
  void testSendMethodCatchBlockWithBufferException() throws Exception {
    // Given: Mock the writeBuffer() itself to throw an exception
    RuntimeException bufferException = new RuntimeException("Buffer write failed");
    when(mockIoSession.writeBuffer(any(Buffer.class))).thenThrow(bufferException);

    // When & Then: Call sendClientProxyMetadata which internally calls send method
    ProxyConnectionException exception = assertThrows(ProxyConnectionException.class, () -> {
      connector.sendClientProxyMetadata(mockClientSession);
    });

    assertEquals("Failed to send data through proxy", exception.getMessage());
    assertEquals(bufferException, exception.getCause());
  }

  @Test
  void testSendMethodWithProxyUserAndPassword() throws Exception {
    // Given: Create connector with credentials and mock to throw exception
    HttpClientConnector connectorWithAuth = new HttpClientConnector(proxyAddress, remoteAddress, "user", "password");
    IOException originalException = new IOException("Authentication failed");
    when(mockWriteFuture.verify(anyLong())).thenThrow(originalException);

    // When & Then: Call sendClientProxyMetadata which will try to authenticate and call send
    ProxyConnectionException exception = assertThrows(ProxyConnectionException.class, () -> {
      connectorWithAuth.sendClientProxyMetadata(mockClientSession);
    });

    assertEquals("Failed to send data through proxy", exception.getMessage());
    assertEquals(originalException, exception.getCause());
  }
}
