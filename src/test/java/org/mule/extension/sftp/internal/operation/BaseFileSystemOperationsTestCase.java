/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.junit.Test;
import org.mule.extension.sftp.internal.exception.IllegalContentException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.ByteArrayInputStream;



@SmallTest
public class BaseFileSystemOperationsTestCase {

  private SftpOperations sftpOperations = new SftpOperations();
  private InputStream stream = new ByteArrayInputStream("Dummy data".getBytes());

  @Test
  public void testDoWriteOPeration() {
    //test for null content
    try {
      sftpOperations.doWrite(null, null, null, null, false, false, null);
    } catch (IllegalContentException e) {
    }

    //test for invalid path
    try {
      sftpOperations.doWrite(null, null, null, stream, false, false, null);
    } catch (IllegalPathException e) {
    }

    //test valid
    //    sftpOperations.doWrite(null, null, "./", stream, true, false, null);

  }
}
