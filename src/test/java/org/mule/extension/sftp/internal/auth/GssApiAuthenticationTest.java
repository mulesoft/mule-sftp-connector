/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import static java.text.MessageFormat.format;
import static org.ietf.jgss.GSSException.BAD_BINDINGS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SmallTest
public class GssApiAuthenticationTest {

  private static GssApiAuthentication mockAuth;
  private static GSSContext mockContext;

  @BeforeEach
  public void setup() {
    mockContext = mock(GSSContext.class);
    mockAuth = mock(GssApiAuthentication.class);
  }

  @Test
  void testStart() throws Exception {
    when(mockAuth.createContext()).thenReturn(mockContext);
    doCallRealMethod().when(mockAuth).start();

    mockAuth.start();
    verify(mockContext).initSecContext(new byte[0], 0, 0);
  }

  @Test
  void testProcess() throws Exception {
    when(mockAuth.createContext()).thenReturn(mockContext);
    when(mockAuth.extractToken(any())).thenReturn("token".getBytes());
    when(mockContext.isEstablished()).thenReturn(true);
    doCallRealMethod().when(mockAuth).start();
    doCallRealMethod().when(mockAuth).process();

    mockAuth.start();
    mockAuth.process();
    verify(mockContext).dispose();
  }

  @Test
  void testProcessExceptionAtExtractToken() throws Exception {
    when(mockAuth.extractToken(any())).thenThrow(new IOException(format("IOException thrown")));
    doCallRealMethod().when(mockAuth).process();

    assertThrows(IOException.class, () -> mockAuth.process());
  }

  @Test
  void testStartNullContext() throws Exception {
    when(mockAuth.createContext()).thenReturn(mockContext);
    doCallRealMethod().when(mockAuth).close();
    doCallRealMethod().when(mockAuth).start();
    doThrow(new GSSException(BAD_BINDINGS)).when(mockContext).requestMutualAuth(anyBoolean());

    assertThrows(GSSException.class, () -> mockAuth.start());
    assertTrue(mockAuth.done);
  }

  @Test
  void testProcessWithNullContext() throws Exception {
    doCallRealMethod().when(mockAuth).process();
    assertThrows(IOException.class, () -> mockAuth.process());
  }
}
