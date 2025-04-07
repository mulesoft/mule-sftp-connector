/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.Test;
import org.mule.extension.sftp.internal.exception.FileLockedException;
import org.mule.extension.sftp.internal.lock.PathLock;
import org.mule.extension.sftp.internal.lock.UriLock;
import org.mule.extension.sftp.internal.operation.*;
import org.mule.tck.size.SmallTest;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
public class FileSystemTestCase {

  private Path path = Paths.get("src/test/resources/sample.jpg");
  private final AbstractExternalFileSystem abstractExternalFileSystem = new AbstractExternalFileSystem("src") {

    @Override
    public void changeToBaseDir() {}

    @Override
    protected ListCommand getListCommand() {
      return null;
    }

    @Override
    protected ReadCommand getReadCommand() {
      return null;
    }

    @Override
    protected WriteCommand getWriteCommand() {
      return null;
    }

    @Override
    protected CopyCommand getCopyCommand() {
      return null;
    }

    @Override
    protected MoveCommand getMoveCommand() {
      return null;
    }

    @Override
    protected DeleteCommand getDeleteCommand() {
      return null;
    }

    @Override
    protected RenameCommand getRenameCommand() {
      return null;
    }

    @Override
    protected CreateDirectoryCommand getCreateDirectoryCommand() {
      return null;
    }

    @Override
    protected UriLock createLock(URI uri) {
      UriLock lock = mock(UriLock.class);
      when(lock.tryLock()).thenReturn(false);
      return lock;
    }
  };

  private final AbstractFileSystem abstractFileSystem = new AbstractFileSystem("src") {

    @Override
    protected ListCommand getListCommand() {
      return null;
    }

    @Override
    protected ReadCommand getReadCommand() {
      return null;
    }

    @Override
    protected WriteCommand getWriteCommand() {
      return null;
    }

    @Override
    protected CopyCommand getCopyCommand() {
      return null;
    }

    @Override
    protected MoveCommand getMoveCommand() {
      return null;
    }

    @Override
    protected DeleteCommand getDeleteCommand() {
      return null;
    }

    @Override
    protected RenameCommand getRenameCommand() {
      return null;
    }

    @Override
    protected CreateDirectoryCommand getCreateDirectoryCommand() {
      return null;
    }

    @Override
    protected PathLock createLock(Path path) {
      PathLock lock = mock(PathLock.class);
      when(lock.tryLock()).thenReturn(false);
      return lock;
    }

    @Override
    public void changeToBaseDir() {}
  };

  @Test(expected = UnsupportedOperationException.class)
  public void testVerifyNotLocked() {
    abstractExternalFileSystem.verifyNotLocked(path);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testLock() {
    abstractExternalFileSystem.lock(path);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testIsLocked() {
    abstractExternalFileSystem.isLocked(path);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCreateLock() {
    abstractExternalFileSystem.createLock(path);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testAcquireLock() {
    abstractExternalFileSystem.acquireLock(abstractFileSystem.createLock(path));
  }

  @Test(expected = FileLockedException.class)
  public void testVerifyNotLocked2() {
    abstractExternalFileSystem.verifyNotLocked(path.toUri());
  }

  @Test(expected = FileLockedException.class)
  public void testVerifyNotLocked3() {
    abstractFileSystem.verifyNotLocked(path);
  }
}
