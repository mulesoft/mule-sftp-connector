/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import java.util.List;
import java.util.function.Consumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mule.runtime.extension.api.runtime.source.PollContext.PollItemStatus.SOURCE_STOPPING;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class SftpDirectorySourceTest {

  private SftpDirectorySource sftpDirectorySource;
  private PollContext<InputStream, SftpFileAttributes> pollContext;
  private ConnectionProvider<SftpFileSystemConnection> connectionProvider;
  private SftpFileSystemConnection mockSFTPFileSystemConnection;
  private RejectPollItem pollItem = new RejectPollItem();
  private PollItemWithError pollItemWithError = new PollItemWithError();

  @Before
  public void setup() throws ConnectionException, NoSuchFieldException, IllegalAccessException, URISyntaxException {
    sftpDirectorySource = new SftpDirectorySource();

    pollContext = mock(PollContext.class);
    when(pollContext.isSourceStopping()).thenReturn(false);

    connectionProvider = mock(ConnectionProvider.class);
    mockSFTPFileSystemConnection = mock(SftpFileSystemConnection.class);
    when(connectionProvider.connect()).thenReturn(mockSFTPFileSystemConnection);

    // Inject the mock provider into the source
    Field providerField = SftpDirectorySource.class.getDeclaredField("fileSystemProvider");
    providerField.setAccessible(true);
    providerField.set(sftpDirectorySource, connectionProvider);

    // Set up directoryUri and config
    Field dirUriField = SftpDirectorySource.class.getDeclaredField("directoryUri");
    dirUriField.setAccessible(true);
    dirUriField.set(sftpDirectorySource, new java.net.URI("/some/dir"));

    Field configField = SftpDirectorySource.class.getDeclaredField("config");
    configField.setAccessible(true);
    configField.set(sftpDirectorySource, mock(org.mule.extension.sftp.internal.extension.SftpConnector.class));

    Field watermarkEnabledField = SftpDirectorySource.class.getDeclaredField("watermarkEnabled");
    watermarkEnabledField.setAccessible(true);
    watermarkEnabledField.set(sftpDirectorySource, true);
  }

  @Test
  public void testPollProcessesFilesSuccessfully()
      throws ConnectionException, NoSuchFieldException, IllegalAccessException, URISyntaxException {
    String fileName = "test";
    Path filePath = Paths.get("/somepath/" + fileName);
    SftpFileAttributes attributes = new SftpFileAttributes();
    FieldUtils.writeField(attributes, "fileName", fileName, true);
    FieldUtils.writeField(attributes, "directory", false, true);
    FieldUtils.writeField(attributes, "path", filePath.toString(), true);
    Result<String, SftpFileAttributes> mockResult =
        Result.<String, SftpFileAttributes>builder().output("test").attributes(attributes).build();
    List<Result<String, SftpFileAttributes>> fileList = java.util.Collections.singletonList(mockResult);
    when(mockSFTPFileSystemConnection.list(any(), anyString(), anyBoolean(), any(), any())).thenReturn(fileList);

    Result<InputStream, SftpFileAttributes> mockReadResult =
        Result.<InputStream, SftpFileAttributes>builder().output(mock(InputStream.class)).attributes(attributes).build();
    when(mockSFTPFileSystemConnection.read(any(), anyString(), anyBoolean(), any())).thenReturn(mockReadResult);

    when(pollContext.accept(any())).then((Answer<PollContext.PollItemStatus>) invocationOnMock -> {
      Consumer<PollContext.PollItem<InputStream, SftpFileAttributes>> pollItemConsumer =
          (Consumer<PollContext.PollItem<InputStream, SftpFileAttributes>>) invocationOnMock.getArguments()[0];
      pollItemConsumer.accept(pollItem);
      sftpDirectorySource.onRejectedItem(((RejectPollItem) pollItem).getResult(), mock(SourceCallbackContext.class));
      return SOURCE_STOPPING;
    });
    sftpDirectorySource.poll(pollContext);

    assertEquals(pollItem.getResult(), mockReadResult);
    verify(connectionProvider, times(1)).disconnect(mockSFTPFileSystemConnection);
  }

  @Test
  public void testErrorsWhileProccessingFilesAreHandledGracefully()
      throws ConnectionException, NoSuchFieldException, IllegalAccessException, URISyntaxException {
    String fileName = "test";
    Path filePath = Paths.get("/somepath/" + fileName);
    SftpFileAttributes attributes = new SftpFileAttributes();
    FieldUtils.writeField(attributes, "fileName", fileName, true);
    FieldUtils.writeField(attributes, "directory", false, true);
    FieldUtils.writeField(attributes, "path", filePath.toString(), true);
    Result<String, SftpFileAttributes> mockResult =
        Result.<String, SftpFileAttributes>builder().output("test").attributes(attributes).build();
    List<Result<String, SftpFileAttributes>> fileList = java.util.Collections.singletonList(mockResult);
    when(mockSFTPFileSystemConnection.list(any(), anyString(), anyBoolean(), any(), any())).thenReturn(fileList);

    Result<InputStream, SftpFileAttributes> mockReadResult =
        Result.<InputStream, SftpFileAttributes>builder().output(mock(InputStream.class)).attributes(attributes).build();
    when(mockSFTPFileSystemConnection.read(any(), anyString(), anyBoolean(), any())).thenReturn(mockReadResult);

    when(pollContext.accept(any())).then((Answer<PollContext.PollItemStatus>) invocationOnMock -> {
      Consumer<PollContext.PollItem<InputStream, SftpFileAttributes>> pollItemConsumer =
          (Consumer<PollContext.PollItem<InputStream, SftpFileAttributes>>) invocationOnMock.getArguments()[0];
      pollItemConsumer.accept(pollItemWithError);
      return SOURCE_STOPPING;
    });

    sftpDirectorySource.poll(pollContext);

    verify(connectionProvider, times(1)).disconnect(mockSFTPFileSystemConnection);
  }

  @Test
  public void testPollWithSourceStopping() {
    when(pollContext.isSourceStopping()).thenReturn(true);
    sftpDirectorySource.poll(pollContext);
    verify(pollContext, times(1)).isSourceStopping();
  }

  @Test(expected = NullPointerException.class)
  public void testPollException() throws NoSuchFieldException, IllegalAccessException {
    Field providerField = SftpDirectorySource.class.getDeclaredField("fileSystemProvider");
    providerField.setAccessible(true);
    providerField.set(sftpDirectorySource, null);

    Field dirUriField = SftpDirectorySource.class.getDeclaredField("directoryUri");
    dirUriField.setAccessible(true);
    dirUriField.set(sftpDirectorySource, null);

    sftpDirectorySource.poll(pollContext);
  }

  @Test
  public void testOnRejectedTime() throws IOException {
    Result<InputStream, SftpFileAttributes> mockResult = mock(Result.class);
    InputStream mockStream = mock(InputStream.class);
    when(mockResult.getOutput()).thenReturn(mockStream);

    sftpDirectorySource.onRejectedItem(mockResult, null);
    verify(mockStream, times(1)).close();
  }

  @Test
  public void testPollRetriesOnChannelBeingClosed()
      throws ConnectionException, NoSuchFieldException, IllegalAccessException, URISyntaxException {
    // Simulate first list call throws, second returns a file
    org.apache.sshd.common.SshException sshCause = new org.apache.sshd.common.SshException("Channel is being closed");
    RuntimeException listException = new RuntimeException("wrapper", sshCause);
    Result<String, SftpFileAttributes> mockResult = mock(Result.class);
    List<Result<String, SftpFileAttributes>> fileList = java.util.Collections.singletonList(mockResult);
    when(mockSFTPFileSystemConnection.list(any(), anyString(), anyBoolean(), any(), any()))
        .thenThrow(listException)
        .thenReturn(fileList);

    // Run poll
    sftpDirectorySource.poll(pollContext);

    // Verify disconnect and list were called
    verify(mockSFTPFileSystemConnection, times(1)).disconnect();
    verify(mockSFTPFileSystemConnection, times(2)).list(any(), anyString(), anyBoolean(), any(), any());
  }

  @Test
  public void testPollHandlesReconnectionFailure() throws Exception {
    when(connectionProvider.connect()).thenReturn(mockSFTPFileSystemConnection)
        .thenThrow(new org.mule.runtime.api.connection.ConnectionException("Reconnection failed"));

    // Simulate channel closed exception
    org.apache.sshd.common.SshException sshCause = new org.apache.sshd.common.SshException("Channel is being closed");
    RuntimeException listException = new RuntimeException("wrapper", sshCause);
    when(mockSFTPFileSystemConnection.list(any(), anyString(), anyBoolean(), any(), any())).thenThrow(listException);

    // Run poll
    sftpDirectorySource.poll(pollContext);

    // Verify disconnect was called and onConnectionException was triggered
    verify(mockSFTPFileSystemConnection, times(1)).disconnect();
    verify(pollContext, times(1)).onConnectionException(any(org.mule.runtime.api.connection.ConnectionException.class));
  }

  @Test
  public void testPollIgnoresNonChannelClosedExceptions() throws Exception {
    // Simulate a different exception (not channel closed)
    RuntimeException otherException = new RuntimeException("Some other SFTP error");
    when(mockSFTPFileSystemConnection.list(any(), anyString(), anyBoolean(), any(), any())).thenThrow(otherException);

    // Run poll - should NOT throw the exception (it gets caught and logged)
    sftpDirectorySource.poll(pollContext);

    // Verify disconnect was NOT called (no reconnection attempt for non-channel-closed exceptions)
    verify(mockSFTPFileSystemConnection, never()).disconnect();
    // The method should complete without throwing, and may call onConnectionException if applicable
  }



  private static class RejectPollItem implements PollContext.PollItem {

    private Result result;

    public Result getResult() {
      return result;
    }

    @Override
    public SourceCallbackContext getSourceCallbackContext() {
      return mock(SourceCallbackContext.class);
    }

    @Override
    public PollContext.PollItem setResult(Result result) {
      this.result = result;
      return this;
    }

    @Override
    public PollContext.PollItem setWatermark(Serializable serializable) {
      return this;
    }

    @Override
    public PollContext.PollItem setId(String s) {
      return this;
    }
  }


  private static class PollItemWithError implements PollContext.PollItem {

    private Result result;

    public Result getResult() {
      return result;
    }

    @Override
    public SourceCallbackContext getSourceCallbackContext() {
      return mock(SourceCallbackContext.class);
    }

    @Override
    public PollContext.PollItem setResult(Result result) {
      throw new RuntimeException("Test error");
    }

    @Override
    public PollContext.PollItem setWatermark(Serializable serializable) {
      return this;
    }

    @Override
    public PollContext.PollItem setId(String s) {
      return this;
    }
  }
}
