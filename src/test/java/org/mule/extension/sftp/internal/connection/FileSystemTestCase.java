/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.lock.PathLock;
import org.mule.tck.size.SmallTest;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SmallTest
public class FileSystemTestCase {

  private Path path = Paths.get("src/test/resources/sample.jpg");
  private final AbstractExternalFileSystem abstractExternalFileSystem = mock(AbstractExternalFileSystem.class);

  private final AbstractFileSystem abstractFileSystem = mock(AbstractFileSystem.class);

  @Test
  void testVerifyNotLocked() {
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.verifyNotLocked(path));
  }

  @Test
  void testIsLocked() {
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.isLocked(path));
  }

  @Test
  void testCreateLock() {
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.createLock(path));
  }

  @Test
  void testAcquireLock() {
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.acquireLock(mock(PathLock.class)));
  }

  //  @Test
  //  void testLock() {
  //    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.lock(path));
  //  }

  //  @Test
  //  void testVerifyNotLockedUriPath() {
  //    when(abstractExternalFileSystem.isLocked(any(URI.class))).thenCallRealMethod();
  //    assertThrows(FileLockedException.class, () -> abstractExternalFileSystem.verifyNotLocked(path.toUri()));
  //  }

  //  @Test
  //  void testVerifyNotLocked3() {
  //    abstractFileSystem.verifyNotLocked(path);
  //  }

  //    @Override
  //    protected UriLock createLock(URI uri) {
  //      UriLock lock = mock(UriLock.class);
  //      when(lock.tryLock()).thenReturn(false);
  //      return lock;
  //    }
  //
  //    @Override
  //    protected PathLock createLock(Path path) {
  //      PathLock lock = mock(PathLock.class);
  //      when(lock.tryLock()).thenReturn(false);
  //      return lock;
  //    }
}
