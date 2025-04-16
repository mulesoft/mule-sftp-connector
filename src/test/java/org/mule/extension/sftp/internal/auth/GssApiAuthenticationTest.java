/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import org.ietf.jgss.GSSContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SmallTest
public class GssApiAuthenticationTest {

  private static GssApiAuthentication authMock;
  private static GssApiAuthentication auth;
  private static GSSContext mockContext;

  @BeforeEach
  public void setup() {
    mockContext = mock(GSSContext.class);
    authMock = mock(GssApiAuthentication.class);
    auth = new GssApiAuthenticationTestImpl(new InetSocketAddress(80));
  }

  @Test
  void testStart() throws Exception {
    when(authMock.createContext()).thenReturn(mockContext);
    authMock.start();
    verify(mockContext).initSecContext(new byte[0], 0, 0);
  }

  @Test
  void testProcess() throws Exception {
    when(authMock.createContext()).thenReturn(mockContext);
    authMock.start();

    when(authMock.extractToken(any())).thenReturn("token".getBytes());
    when(mockContext.isEstablished()).thenReturn(true);
    authMock.process();
    verify(mockContext).dispose();
  }

  @Test
  void testProcessNullToken() throws Exception {
    when(authMock.createContext()).thenReturn(mockContext);
    authMock.start();

    assertThrows(NullPointerException.class, () -> authMock.process());
  }

  @Test
  void testStartNullContext() {
    assertThrows(NullPointerException.class, () -> auth.start());
    assertEquals(true, auth.done);
  }

  @Test
  void testProcessNullContext() {
    assertThrows(IOException.class, () -> auth.process());
  }

  static class GssApiAuthenticationTestImpl extends GssApiAuthentication {

    public GssApiAuthenticationTestImpl(InetSocketAddress proxy) {
      super(proxy);
    }

    @Override
    public Object getToken() throws Exception {
      return null;
    }

    @Override
    protected GSSContext createContext() throws Exception {
      return null;
    }

    @Override
    protected byte[] extractToken(Object input) throws Exception {
      return new byte[0];
    }
  }
}
