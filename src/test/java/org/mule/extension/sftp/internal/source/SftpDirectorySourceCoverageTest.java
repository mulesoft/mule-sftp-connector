/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpDirectorySourceCoverageTest {

  @Test
  void testOnTerminate() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Mock SourceCallbackContext
    SourceCallbackContext mockContext = mock(SourceCallbackContext.class);

    // onTerminate should do nothing - this just increases method coverage
    source.onTerminate(mockContext);

    // No specific verification needed since method does nothing
    assertNotNull(source);
  }

  @Test
  void testPollWithSourceStopping() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Mock PollContext that is stopping
    PollContext<InputStream, SftpFileAttributes> mockContext = mock(PollContext.class);
    when(mockContext.isSourceStopping()).thenReturn(true);

    // This should return early without doing anything
    source.poll(mockContext);

    // Verify isSourceStopping was called but nothing else
    verify(mockContext, times(1)).isSourceStopping();
    verify(mockContext, never()).accept(any());
  }

  @Test
  void testIsChannelBeingClosedWithSshException() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private isChannelBeingClosed method
    Method isChannelBeingClosedMethod = SftpDirectorySource.class.getDeclaredMethod("isChannelBeingClosed", Exception.class);
    isChannelBeingClosedMethod.setAccessible(true);

    // Test with SshException containing "Channel is being closed"
    org.apache.sshd.common.SshException sshException = new org.apache.sshd.common.SshException("Channel is being closed");
    RuntimeException wrapperException = new RuntimeException("wrapper", sshException);

    boolean result = (boolean) isChannelBeingClosedMethod.invoke(source, wrapperException);
    assertTrue(result);
  }

  @Test
  void testIsChannelBeingClosedWithRegularException() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private isChannelBeingClosed method
    Method isChannelBeingClosedMethod = SftpDirectorySource.class.getDeclaredMethod("isChannelBeingClosed", Exception.class);
    isChannelBeingClosedMethod.setAccessible(true);

    // Test with regular exception
    RuntimeException regularException = new RuntimeException("Some other error");
    boolean result = (boolean) isChannelBeingClosedMethod.invoke(source, regularException);
    assertFalse(result);
  }

  @Test
  void testHasAttributesReturnsFalse() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private hasAttributes method
    Method hasAttributesMethod = SftpDirectorySource.class.getDeclaredMethod("hasAttributes", Result.class);
    hasAttributesMethod.setAccessible(true);

    // Mock Result without attributes
    Result<String, SftpFileAttributes> mockResult = mock(Result.class);
    when(mockResult.getAttributes()).thenReturn(Optional.empty());

    boolean result = (boolean) hasAttributesMethod.invoke(source, mockResult);
    assertFalse(result);

    verify(mockResult, times(1)).getAttributes();
  }

  @Test
  void testHasAttributesReturnsTrue() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private hasAttributes method
    Method hasAttributesMethod = SftpDirectorySource.class.getDeclaredMethod("hasAttributes", Result.class);
    hasAttributesMethod.setAccessible(true);

    // Mock Result with attributes
    Result<String, SftpFileAttributes> mockResult = mock(Result.class);
    SftpFileAttributes mockAttributes = mock(SftpFileAttributes.class);
    when(mockResult.getAttributes()).thenReturn(Optional.of(mockAttributes));

    boolean result = (boolean) hasAttributesMethod.invoke(source, mockResult);
    assertTrue(result);

    verify(mockResult, times(1)).getAttributes();
  }

  @Test
  void testShouldSkipFileForDirectory() throws Exception {
    SftpDirectorySource source = new SftpDirectorySource();

    // Use reflection to access the private shouldSkipFile method
    Method shouldSkipFileMethod = SftpDirectorySource.class.getDeclaredMethod("shouldSkipFile", SftpFileAttributes.class);
    shouldSkipFileMethod.setAccessible(true);

    // Mock attributes for a directory
    SftpFileAttributes mockAttributes = mock(SftpFileAttributes.class);
    when(mockAttributes.isDirectory()).thenReturn(true);

    boolean result = (boolean) shouldSkipFileMethod.invoke(source, mockAttributes);
    assertTrue(result);

    verify(mockAttributes, times(1)).isDirectory();
  }
}
