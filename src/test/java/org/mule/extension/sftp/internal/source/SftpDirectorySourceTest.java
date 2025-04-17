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

import java.io.IOException;
import java.io.InputStream;

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
}
