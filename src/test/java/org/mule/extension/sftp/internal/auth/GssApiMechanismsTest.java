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
  void testCloseContextSilently() throws GSSException {
    GSSContext mockContext = mock(GSSContext.class);
    GssApiMechanisms.closeContextSilently(mockContext);
    verify(mockContext).dispose();
  }

  @Test
  void testGetCanonicalName() throws GSSException {
    String s = GssApiMechanisms.getCanonicalName(new InetSocketAddress(8080));
    assertEquals("0.0.0.0", s);
  }

  @Test
  void testWorked() {
    GssApiMechanisms.getSupportedMechanisms();
    GssApiMechanisms.worked(GssApiMechanisms.SPNEGO);
    assertEquals(2, GssApiMechanisms.getSupportedMechanisms().size());
  }

}
