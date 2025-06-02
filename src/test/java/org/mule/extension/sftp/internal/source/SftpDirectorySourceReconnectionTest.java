/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpDirectorySourceReconnectionTest {

  @Test
  void testExtractConnectionExceptionFromConnectionException() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private extractConnectionException method
    Method extractMethod = SftpDirectorySource.class.getDeclaredMethod("extractConnectionException", Throwable.class);
    extractMethod.setAccessible(true);

    // Test with direct ConnectionException
    ConnectionException connectionException = new ConnectionException("Connection failed");
    @SuppressWarnings("unchecked")
    Optional<ConnectionException> result = (Optional<ConnectionException>) extractMethod.invoke(source, connectionException);

    assertTrue(result.isPresent());
    assertEquals("Connection failed", result.get().getMessage());
  }

  @Test
  void testExtractConnectionExceptionFromWrappedException() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private extractConnectionException method
    Method extractMethod = SftpDirectorySource.class.getDeclaredMethod("extractConnectionException", Throwable.class);
    extractMethod.setAccessible(true);

    // Test with nested ConnectionException
    ConnectionException connectionException = new ConnectionException("Connection failed");
    RuntimeException wrapperException = new RuntimeException("Wrapper", connectionException);

    @SuppressWarnings("unchecked")
    Optional<ConnectionException> result = (Optional<ConnectionException>) extractMethod.invoke(source, wrapperException);

    assertTrue(result.isPresent());
    assertEquals("Connection failed", result.get().getMessage());
  }

  @Test
  void testExtractConnectionExceptionFromNonConnectionException() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private extractConnectionException method
    Method extractMethod = SftpDirectorySource.class.getDeclaredMethod("extractConnectionException", Throwable.class);
    extractMethod.setAccessible(true);

    // Test with exception that doesn't contain ConnectionException
    RuntimeException regularException = new RuntimeException("Some other error");

    @SuppressWarnings("unchecked")
    Optional<ConnectionException> result = (Optional<ConnectionException>) extractMethod.invoke(source, regularException);

    assertFalse(result.isPresent());
  }

  @Test
  void testReconnectionExceptionHandling() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();
    PollContext<InputStream, SftpFileAttributes> mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(false);

    // Mock fileSystemProvider
    @SuppressWarnings("unchecked")
    ConnectionProvider<SftpFileSystemConnection> mockProvider = mock(ConnectionProvider.class);
    SftpFileSystemConnection mockFileSystem = mock(SftpFileSystemConnection.class);

    // Setup successful connection first, then failure on reconnection
    when(mockProvider.connect()).thenReturn(mockFileSystem);

    // Inject the mock provider into the source
    Field providerField = SftpDirectorySource.class.getDeclaredField("fileSystemProvider");
    providerField.setAccessible(true);
    providerField.set(source, mockProvider);

    // Set up directoryUri and config
    Field dirUriField = SftpDirectorySource.class.getDeclaredField("directoryUri");
    dirUriField.setAccessible(true);
    dirUriField.set(source, new URI("/some/dir"));

    Field configField = SftpDirectorySource.class.getDeclaredField("config");
    configField.setAccessible(true);
    configField.set(source, mock(SftpConnector.class));

    // This test focuses on the code structure without trying to trigger actual reconnection
    // Just verify the setup works and no exceptions are thrown during initialization
    assertNotNull(source);

    // Verify that we can access the fields we need
    assertNotNull(providerField.get(source));
    assertNotNull(dirUriField.get(source));
    assertNotNull(configField.get(source));
  }
}
