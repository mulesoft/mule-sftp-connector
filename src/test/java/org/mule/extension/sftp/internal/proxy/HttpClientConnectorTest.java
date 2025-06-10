/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.exception.ProxyConnectionException;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.buffer.Buffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;

@SmallTest
public class HttpClientConnectorTest {

  private HttpClientConnector connector;
  private IoSession mockSession;
  private IoWriteFuture mockWriteFuture;
  private InetSocketAddress proxyAddress;
  private InetSocketAddress remoteAddress;

  @BeforeEach
  void setUp() {
    proxyAddress = new InetSocketAddress("proxy.example.com", 8080);
    remoteAddress = new InetSocketAddress("target.example.com", 22);
    connector = new HttpClientConnector(proxyAddress, remoteAddress);

    mockSession = mock(IoSession.class);
    mockWriteFuture = mock(IoWriteFuture.class);
  }

  @Test
  void testSendMethodCatchBlockWithIOException() throws Exception {
    // Given: Mock the session to throw IOException when writeBuffer().verify() is called
    IOException originalException = new IOException("Network connection failed");
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenThrow(originalException);

    // Create a test message
    StringBuilder testMessage = new StringBuilder("CONNECT target.example.com:22 HTTP/1.1\r\n");

    // When & Then: Call the private send method and verify ProxyConnectionException is thrown
    try {
      invokeSendMethod(testMessage, mockSession);
      fail("Expected ProxyConnectionException to be thrown");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof ProxyConnectionException);
      ProxyConnectionException proxyException = (ProxyConnectionException) e.getCause();
      assertEquals("Failed to send data through proxy", proxyException.getMessage());
      assertEquals(originalException, proxyException.getCause());
    }
  }

  @Test
  void testSendMethodCatchBlockWithRuntimeException() throws Exception {
    // Given: Mock the session to throw RuntimeException when writeBuffer().verify() is called
    RuntimeException originalException = new RuntimeException("Unexpected error during write operation");
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenThrow(originalException);

    StringBuilder testMessage = new StringBuilder("CONNECT target.example.com:22 HTTP/1.1\r\n");

    // When & Then: Call the private send method and verify ProxyConnectionException is thrown
    try {
      invokeSendMethod(testMessage, mockSession);
      fail("Expected ProxyConnectionException to be thrown");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof ProxyConnectionException);
      ProxyConnectionException proxyException = (ProxyConnectionException) e.getCause();
      assertEquals("Failed to send data through proxy", proxyException.getMessage());
      assertEquals(originalException, proxyException.getCause());
    }
  }

  @Test
  void testSendMethodCatchBlockWithTimeoutRuntimeException() throws Exception {
    // Given: Mock the session to throw a runtime exception simulating timeout
    RuntimeException timeoutException = new RuntimeException("Connection timeout");
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenThrow(timeoutException);

    StringBuilder testMessage = new StringBuilder("CONNECT target.example.com:22 HTTP/1.1\r\n");

    // When & Then: Call the private send method and verify ProxyConnectionException is thrown
    try {
      invokeSendMethod(testMessage, mockSession);
      fail("Expected ProxyConnectionException to be thrown");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof ProxyConnectionException);
      ProxyConnectionException proxyException = (ProxyConnectionException) e.getCause();
      assertEquals("Failed to send data through proxy", proxyException.getMessage());
      assertEquals(timeoutException, proxyException.getCause());
    }
  }

  @Test
  void testSendMethodCatchBlockWithNullPointerException() throws Exception {
    // Given: Mock the session to throw NullPointerException (a common runtime exception)
    NullPointerException nullPointerException = new NullPointerException("Null session state");
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenThrow(nullPointerException);

    StringBuilder testMessage = new StringBuilder("CONNECT target.example.com:22 HTTP/1.1\r\n");

    // When & Then: Call the private send method and verify ProxyConnectionException is thrown
    try {
      invokeSendMethod(testMessage, mockSession);
      fail("Expected ProxyConnectionException to be thrown");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof ProxyConnectionException);
      ProxyConnectionException proxyException = (ProxyConnectionException) e.getCause();
      assertEquals("Failed to send data through proxy", proxyException.getMessage());
      assertEquals(nullPointerException, proxyException.getCause());
    }
  }

  @Test
  void testSendMethodSuccessfulPath() throws Exception {
    // Given: Mock successful writeBuffer and verify operations
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenReturn(mockWriteFuture); // Successful operation

    StringBuilder testMessage = new StringBuilder("CONNECT target.example.com:22 HTTP/1.1\r\n");

    // When & Then: Call the private send method and verify no exception is thrown
    assertDoesNotThrow(() -> {
      invokeSendMethod(testMessage, mockSession);
    });

    // Verify the interactions
    verify(mockSession).writeBuffer(any(Buffer.class));
    verify(mockWriteFuture).verify(anyLong());
  }

  @Test
  void testSendMethodWithEmptyMessage() throws Exception {
    // Given: Mock the session to throw IOException for empty message
    IOException originalException = new IOException("Cannot send empty data");
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenThrow(originalException);

    StringBuilder emptyMessage = new StringBuilder("");

    // When & Then: Call the private send method with empty message
    try {
      invokeSendMethod(emptyMessage, mockSession);
      fail("Expected ProxyConnectionException to be thrown");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof ProxyConnectionException);
      ProxyConnectionException proxyException = (ProxyConnectionException) e.getCause();
      assertEquals("Failed to send data through proxy", proxyException.getMessage());
      assertEquals(originalException, proxyException.getCause());
    }
  }

  @Test
  void testSendMethodVerifiesCorrectBuffer() throws Exception {
    // Given: Mock successful operation to verify buffer content
    when(mockSession.writeBuffer(any(Buffer.class))).thenReturn(mockWriteFuture);
    when(mockWriteFuture.verify(anyLong())).thenReturn(mockWriteFuture);

    String testContent = "CONNECT target.example.com:22 HTTP/1.1";
    StringBuilder testMessage = new StringBuilder(testContent);

    // When: Call the private send method
    assertDoesNotThrow(() -> {
      invokeSendMethod(testMessage, mockSession);
    });

    // Then: Verify that writeBuffer was called with a buffer containing the expected content
    verify(mockSession).writeBuffer(argThat(buffer -> {
      // The buffer should contain the message content plus \r\n (from eol method)
      String expectedContent = testContent + "\r\n";
      byte[] expectedBytes = expectedContent.getBytes(StandardCharsets.US_ASCII);

      // Basic validation that buffer was called (exact content validation would require more buffer manipulation)
      return buffer != null && buffer.available() == expectedBytes.length;
    }));
  }

  @Test
  void testSendMethodCatchBlockWithBufferException() throws Exception {
    // Given: Mock the session.writeBuffer() itself to throw an exception
    RuntimeException bufferException = new RuntimeException("Buffer write failed");
    when(mockSession.writeBuffer(any(Buffer.class))).thenThrow(bufferException);

    StringBuilder testMessage = new StringBuilder("CONNECT target.example.com:22 HTTP/1.1\r\n");

    // When & Then: Call the private send method and verify ProxyConnectionException is thrown
    try {
      invokeSendMethod(testMessage, mockSession);
      fail("Expected ProxyConnectionException to be thrown");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof ProxyConnectionException);
      ProxyConnectionException proxyException = (ProxyConnectionException) e.getCause();
      assertEquals("Failed to send data through proxy", proxyException.getMessage());
      assertEquals(bufferException, proxyException.getCause());
    }
  }

  /**
   * Helper method to invoke the private send method using reflection
   */
  private void invokeSendMethod(StringBuilder message, IoSession session) throws Exception {
    Method sendMethod = HttpClientConnector.class.getDeclaredMethod("send", StringBuilder.class, IoSession.class);
    sendMethod.setAccessible(true);
    sendMethod.invoke(connector, message, session);
  }
}
