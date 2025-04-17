/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusLineTest {

  @Test
  void testStatusLineConstructor() {
    StatusLine statusLine = new StatusLine("1", 1, "test");
    assertEquals("test", statusLine.getReason());
    assertEquals("1", statusLine.getVersion());
  }

}
