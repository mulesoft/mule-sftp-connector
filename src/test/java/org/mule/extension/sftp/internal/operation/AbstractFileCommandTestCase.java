/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.junit.Test;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.tck.size.SmallTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SmallTest
public class AbstractFileCommandTestCase {

  static class AbstractFileCommandStub extends AbstractFileCommand {

    protected AbstractFileCommandStub(FileSystem fileSystem) {
      super(fileSystem);
    }

    @Override
    protected boolean exists(Object path) {
      return false;
    }

    @Override
    protected Object getParent(Object path) {
      return null;
    }

    @Override
    protected Object getBasePath(FileSystem fileSystem) {
      return null;
    }

    @Override
    protected Object resolvePath(Object basePath, String filePath) {
      return null;
    }

    @Override
    protected Object getAbsolutePath(Object path) {
      return null;
    }

    @Override
    protected String pathToString(Object path) {
      return null;
    }

    @Override
    protected void doMkDirs(Object directoryPath) {

    }
  }

  @Test
  public void testAbstractFileCommand() {
    AbstractFileCommandStub stub = new AbstractFileCommandStub(null);
    assertEquals("exception", stub.exception("exception").getMessage());
    assertEquals("exception", stub.exception("exception", new Exception()).getMessage());

    IllegalPathException ex = stub.cannotReadDirectoryException("path");
    assertTrue(ex instanceof IllegalPathException);

    IllegalPathException exception = stub.cannotListFileException("path");
    assertTrue(exception instanceof IllegalPathException);
  }

}
