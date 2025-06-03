/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import java.util.List;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SmallTest
class SftpDirectorySourceTest {

  private static SftpDirectorySource sftpDirectorySource;

  @BeforeAll
  static void setup() {
    sftpDirectorySource = spy(SftpDirectorySource.class);
  }

  @Test
  void testPollWithSourceStopping() {
    PollContext mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(true);
    sftpDirectorySource.poll(mockContext);
    verify(mockContext, times(1)).isSourceStopping();
  }

  @Test
  void testPollException() {
    PollContext mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(false);
    assertThrows(NullPointerException.class, () -> sftpDirectorySource.poll(mockContext));
  }

  @Test
  void testOnRejectedTime() throws IOException {
    Result<InputStream, SftpFileAttributes> mockResult = mock(Result.class);
    InputStream mockStream = mock(InputStream.class);
    when(mockResult.getOutput()).thenReturn(mockStream);

    sftpDirectorySource.onRejectedItem(mockResult, null);
    verify(mockStream, times(1)).close();
  }

  @Test
  void testPollRetriesOnChannelBeingClosed() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();
    PollContext<InputStream, SftpFileAttributes> mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(false);

    // Mock fileSystemProvider and fileSystem
    @SuppressWarnings("unchecked")
    ConnectionProvider<SftpFileSystemConnection> mockProvider = mock(ConnectionProvider.class);
    SftpFileSystemConnection mockFileSystem = mock(SftpFileSystemConnection.class);
    when(mockProvider.connect()).thenReturn(mockFileSystem);

    // Inject the mock provider into the source
    Field providerField = SftpDirectorySource.class.getDeclaredField("fileSystemProvider");
    providerField.setAccessible(true);
    providerField.set(source, mockProvider);

    // Set up directoryUri and config
    Field dirUriField = SftpDirectorySource.class.getDeclaredField("directoryUri");
    dirUriField.setAccessible(true);
    dirUriField.set(source, new java.net.URI("/some/dir"));

    Field configField = SftpDirectorySource.class.getDeclaredField("config");
    configField.setAccessible(true);
    configField.set(source, mock(org.mule.extension.sftp.internal.extension.SftpConnector.class));

    // Simulate first list call throws, second returns a file
    org.apache.sshd.common.SshException sshCause = new org.apache.sshd.common.SshException("Channel is being closed");
    RuntimeException listException = new RuntimeException("wrapper", sshCause);
    Result<String, SftpFileAttributes> mockResult = mock(Result.class);
    List<Result<String, SftpFileAttributes>> fileList = java.util.Collections.singletonList(mockResult);
    when(mockFileSystem.list(any(), anyString(), anyBoolean(), any(), any()))
        .thenThrow(listException)
        .thenReturn(fileList);

    // Run poll
    source.poll(mockContext);

    // Verify disconnect and list were called
    verify(mockFileSystem, times(1)).disconnect();
    verify(mockFileSystem, times(2)).list(any(), anyString(), anyBoolean(), any(), any());
  }

  @Test
  void testPollHandlesReconnectionFailure() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();
    PollContext<InputStream, SftpFileAttributes> mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(false);

    // Mock fileSystemProvider and fileSystem
    @SuppressWarnings("unchecked")
    ConnectionProvider<SftpFileSystemConnection> mockProvider = mock(ConnectionProvider.class);
    SftpFileSystemConnection mockFileSystem = mock(SftpFileSystemConnection.class);
    when(mockProvider.connect()).thenReturn(mockFileSystem)
        .thenThrow(new org.mule.runtime.api.connection.ConnectionException("Reconnection failed"));

    // Inject the mock provider into the source
    Field providerField = SftpDirectorySource.class.getDeclaredField("fileSystemProvider");
    providerField.setAccessible(true);
    providerField.set(source, mockProvider);

    // Set up directoryUri and config
    Field dirUriField = SftpDirectorySource.class.getDeclaredField("directoryUri");
    dirUriField.setAccessible(true);
    dirUriField.set(source, new java.net.URI("/some/dir"));

    Field configField = SftpDirectorySource.class.getDeclaredField("config");
    configField.setAccessible(true);
    configField.set(source, mock(org.mule.extension.sftp.internal.extension.SftpConnector.class));

    // Simulate channel closed exception
    org.apache.sshd.common.SshException sshCause = new org.apache.sshd.common.SshException("Channel is being closed");
    RuntimeException listException = new RuntimeException("wrapper", sshCause);
    when(mockFileSystem.list(any(), anyString(), anyBoolean(), any(), any())).thenThrow(listException);

    // Run poll
    source.poll(mockContext);

    // Verify disconnect was called and onConnectionException was triggered
    verify(mockFileSystem, times(1)).disconnect();
    verify(mockContext, times(1)).onConnectionException(any(org.mule.runtime.api.connection.ConnectionException.class));
  }

  @Test
  void testPollIgnoresNonChannelClosedExceptions() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();
    PollContext<InputStream, SftpFileAttributes> mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(false);

    // Mock fileSystemProvider and fileSystem
    @SuppressWarnings("unchecked")
    ConnectionProvider<SftpFileSystemConnection> mockProvider = mock(ConnectionProvider.class);
    SftpFileSystemConnection mockFileSystem = mock(SftpFileSystemConnection.class);
    when(mockProvider.connect()).thenReturn(mockFileSystem);

    // Inject the mock provider into the source
    Field providerField = SftpDirectorySource.class.getDeclaredField("fileSystemProvider");
    providerField.setAccessible(true);
    providerField.set(source, mockProvider);

    // Set up directoryUri and config
    Field dirUriField = SftpDirectorySource.class.getDeclaredField("directoryUri");
    dirUriField.setAccessible(true);
    dirUriField.set(source, new java.net.URI("/some/dir"));

    Field configField = SftpDirectorySource.class.getDeclaredField("config");
    configField.setAccessible(true);
    configField.set(source, mock(org.mule.extension.sftp.internal.extension.SftpConnector.class));

    // Simulate a different exception (not channel closed)
    RuntimeException otherException = new RuntimeException("Some other SFTP error");
    when(mockFileSystem.list(any(), anyString(), anyBoolean(), any(), any())).thenThrow(otherException);

    // Run poll - should throw the exception without attempting reconnection
    assertThrows(RuntimeException.class, () -> source.poll(mockContext));

    // Verify disconnect was NOT called (no reconnection attempt)
    verify(mockFileSystem, never()).disconnect();
    verify(mockContext, never()).onConnectionException(any());
  }
}
