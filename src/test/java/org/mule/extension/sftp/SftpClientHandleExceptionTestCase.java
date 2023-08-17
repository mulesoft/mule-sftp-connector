/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.extension.sftp.internal.connection.SftpClient;

import java.util.Arrays;
import java.util.Collection;

import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SftpClientHandleExceptionTestCase {

  private final SftpClient sftpMuleClient;
  private final String errorMessage;
  private final int sftpErrorCode;
  private final Class<? extends Exception> expectedException;

  private static final String EMPTY = "";

  public SftpClientHandleExceptionTestCase(SftpClient sftpMuleClient, String errorMessage, int sftpErrorCode,
                                           Class<? extends Exception> expectedException) {
    this.sftpMuleClient = sftpMuleClient;
    this.errorMessage = errorMessage;
    this.sftpErrorCode = sftpErrorCode;
    this.expectedException = expectedException;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {mock(SftpClient.class), EMPTY, SftpConstants.SSH_FX_NO_SUCH_FILE,
            SftpException.class},
        {mock(SftpClient.class), EMPTY, SftpConstants.SSH_FX_PERMISSION_DENIED,
            SftpException.class},
        {mock(SftpClient.class), EMPTY, SftpConstants.SSH_FX_CONNECTION_LOST,
            SftpConnectionException.class}
    });
  }

  @Test
  public void expectConnectionExceptionWhenHandleException() {
    when(sftpMuleClient.handleException(anyString(), any(Exception.class))).thenCallRealMethod();
    RuntimeException exception = sftpMuleClient.handleException(errorMessage, new SftpException(sftpErrorCode, EMPTY));

    Assert.assertEquals(expectedException, exception.getCause().getClass());
  }
}
