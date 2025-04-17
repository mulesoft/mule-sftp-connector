/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.connection.MuleSftpClient;
import org.mule.extension.sftp.internal.exception.IllegalContentException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SmallTest
public class BaseFileSystemOperationsTestCase {

  private static SftpOperations sftpOperations;

  @BeforeAll
  static void setup() {
    sftpOperations = new SftpOperations();
  }

  @Test
  void testDoWriteOperationNullContent() {
    assertThrows(IllegalContentException.class, () -> sftpOperations.doWrite(null, null, null, null, false, false, null));
  }

  @Test
  void testDoWriteOperationInvalidPath() {
    InputStream stream = new ByteArrayInputStream("Dummy data".getBytes());
    assertThrows(IllegalPathException.class, () -> sftpOperations.doWrite(null, null, null, stream, false, false, null));
  }
}
