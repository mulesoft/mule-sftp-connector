/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.extension.sftp.internal.lock.UriLock;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import org.mule.runtime.extension.api.runtime.operation.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SftpInputStreamTestCase {

  public static final String STREAM_CONTENT = "My stream content";

  @Mock
  private UriLock uriLock;

  @Mock
  private SftpInputStream.SftpFileInputStreamSupplier streamSupplier;

  @Mock
  private ConnectionHandler connectionHandler;


  @Before
  public void setUp() throws Exception {
    when(uriLock.isLocked()).thenReturn(true);
    doAnswer(invocation -> {
      when(uriLock.isLocked()).thenReturn(false);
      return null;
    }).when(uriLock).release();

    when(streamSupplier.get()).thenReturn(new ByteArrayInputStream(STREAM_CONTENT.getBytes(UTF_8)));
  }

  @Test
  public void testSftpInputStream() throws ConnectionException {
    SftpInputStream s = SftpInputStream.newInstance(new SftpConnector(), new SftpFileAttributes(), null, 0L);
    assertEquals(Optional.empty(), Result.<InputStream, SftpFileAttributes>builder().output(s).build().getAttributes());
  }

  @Test
  public void readLockReleasedOnContentConsumed() throws Exception {
    SftpInputStream inputStream = new SftpInputStream(streamSupplier, uriLock);

    verifyZeroInteractions(uriLock);
    assertThat(inputStream.isLocked(), is(true));
    verify(uriLock).isLocked();

    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");

    verify(uriLock, times(1)).release();
    assertThat(inputStream.isLocked(), is(false));
    verify(streamSupplier).releaseConnectionUsedForContentInputStream();
  }

  @Test
  public void readLockReleasedOnEarlyClose() throws Exception {
    SftpInputStream inputStream = new SftpInputStream(streamSupplier, uriLock);

    verifyZeroInteractions(uriLock);
    assertThat(inputStream.isLocked(), is(true));
    verify(uriLock).isLocked();

    inputStream.close();

    verify(uriLock, times(1)).release();
    assertThat(inputStream.isLocked(), is(false));
  }

}
