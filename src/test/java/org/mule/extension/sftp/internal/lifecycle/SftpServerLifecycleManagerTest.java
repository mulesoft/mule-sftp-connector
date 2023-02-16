/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lifecycle;

import junit.framework.TestCase;

public class SftpServerLifecycleManagerTest extends TestCase {

  public void testCreateServer() throws Exception {
    SftpServerLifecycleManager.startSftpServer("5050");
  }
}
