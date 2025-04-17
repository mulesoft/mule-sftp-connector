/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.lock.PathLock;
import org.mule.tck.size.SmallTest;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SmallTest
public class FileSystemTestCase {

  private static Path path;
  private static AbstractExternalFileSystem abstractExternalFileSystem;

  @BeforeAll
  static void setup() {
    path = Paths.get("src/test/resources/sample.jpg");
    abstractExternalFileSystem = mock(AbstractExternalFileSystem.class);
  }

  @Test
  void testVerifyNotLocked() {
    doCallRealMethod().when(abstractExternalFileSystem).verifyNotLocked(any(Path.class));
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.verifyNotLocked(path));
  }

  @Test
  void testIsLocked() {
    doCallRealMethod().when(abstractExternalFileSystem).isLocked(any(Path.class));
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.isLocked(path));
  }

  @Test
  void testCreateLock() {
    doCallRealMethod().when(abstractExternalFileSystem).createLock(any(Path.class));
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.createLock(path));
  }

  @Test
  void testAcquireLock() {
    doCallRealMethod().when(abstractExternalFileSystem).acquireLock(any(PathLock.class));
    assertThrows(UnsupportedOperationException.class, () -> abstractExternalFileSystem.acquireLock(mock(PathLock.class)));
  }
}
