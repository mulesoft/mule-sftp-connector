/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SmallTest
class SftpDirectorySourceTest {

  @Test
  public void testPollWithSourceStopping() {
    SftpDirectorySource sftpDirectorySource = spy(SftpDirectorySource.class);
    PollContext context = mock(PollContext.class);
    when(context.isSourceStopping()).thenReturn(true);
    sftpDirectorySource.poll(context);
  }

  @Test
  public void testPollException() {

    SftpDirectorySource sftpDirectorySource = spy(SftpDirectorySource.class);
    PollContext context = mock(PollContext.class);
//    ConnectionProvider provider = mock(ConnectionProvider.class);
//    when(provider.connect()).thenReturn(mock(SftpFileSystemConnection.class));
    when(context.isSourceStopping()).thenReturn(false);
    assertThrows(NullPointerException.class, () -> sftpDirectorySource.poll(context));
  }

//  @Test
//  public void testPollExceptionInOpenConnection() {
//    SftpFileSystemConnection mockConnection = mock(SftpFileSystemConnection.class);
//
//
//    SftpDirectorySource sftpDirectorySource = spy(SftpDirectorySource.class); doReturn(mockConnection).when(sftpDirectorySource).openConnection(any());
//    PollContext context = mock(PollContext.class);
//    when().thenReturn(mock(SftpFileSystemConnection.class));
//    when(context.isSourceStopping()).thenReturn(false);
//    assertThrows(NullPointerException.class, () -> sftpDirectorySource.poll(context));
//  }
}
