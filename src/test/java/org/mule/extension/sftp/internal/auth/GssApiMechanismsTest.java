/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import org.ietf.jgss.*;
import org.junit.jupiter.api.Test;
import org.mule.tck.size.SmallTest;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SmallTest
public class GssApiMechanismsTest {

  @Test
  public void testCloseContextSilently() throws GSSException {
    GSSContext mockContext = mock(GSSContext.class);
    GssApiMechanisms.closeContextSilently(mockContext);
    verify(mockContext).dispose();
  }

  @Test
  public void testGetCanonicalName() throws GSSException {
    InetSocketAddress inetSocketAddress = new InetSocketAddress(8080);
    String s = GssApiMechanisms.getCanonicalName(inetSocketAddress);
    assertEquals("0.0.0.0", s);
  }

  @Test
  public void testWorked() throws GSSException {
    GssApiMechanisms.getSupportedMechanisms();
    GssApiMechanisms.worked(new Oid("1.3.6.1.5.5.2"));
  }

}
