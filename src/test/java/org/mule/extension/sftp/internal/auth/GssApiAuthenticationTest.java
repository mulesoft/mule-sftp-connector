/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import org.ietf.jgss.GSSException;
import org.junit.Test;
import org.mule.tck.size.SmallTest;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

@SmallTest
public class GssApiAuthenticationTest {

  @Test
  public void test() throws GSSException {
    InetSocketAddress inetSocketAddress = new InetSocketAddress(8080);
    String s = GssApiMechanisms.getCanonicalName(inetSocketAddress);
    assertEquals(s, "0.0.0.0");
  }
}
