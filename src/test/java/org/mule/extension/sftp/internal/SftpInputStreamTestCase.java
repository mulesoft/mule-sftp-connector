/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;


import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.runtime.api.connection.ConnectionHandler;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SftpInputStreamTestCase {

  public static final String STREAM_CONTENT = "My stream content";

  @Mock
  private PathLock pathLock;

  @Mock
  private SftpInputStream.ConnectionAwareSupplier connectionAwareSupplier;

  @Mock
  private ConnectionHandler connectionHandler;


  @Before
  public void setUp() throws Exception {
    when(pathLock.isLocked()).thenReturn(true);
    doAnswer(invocation -> {
      when(pathLock.isLocked()).thenReturn(false);
      return null;
    }).when(pathLock).release();

    when(connectionAwareSupplier.getConnectionHandler()).thenReturn(connectionHandler);
    when(connectionAwareSupplier.get()).thenReturn(new ByteArrayInputStream(STREAM_CONTENT.getBytes(UTF_8)));
  }

  @Test
  public void readLockReleasedOnContentConsumed() throws Exception {
    SftpInputStream inputStream = new SftpInputStream(connectionAwareSupplier, pathLock);

    verifyZeroInteractions(pathLock);
    assertThat(inputStream.isLocked(), is(true));
    verify(pathLock).isLocked();

    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");

    verify(pathLock, times(1)).release();
    assertThat(inputStream.isLocked(), is(false));
    verify(connectionHandler).release();
  }

  @Test
  public void readLockReleasedOnEarlyClose() throws Exception {
    SftpInputStream inputStream = new SftpInputStream(connectionAwareSupplier, pathLock);

    verifyZeroInteractions(pathLock);
    assertThat(inputStream.isLocked(), is(true));
    verify(pathLock).isLocked();

    inputStream.close();

    verify(pathLock, times(1)).release();
    assertThat(inputStream.isLocked(), is(false));
  }

}
