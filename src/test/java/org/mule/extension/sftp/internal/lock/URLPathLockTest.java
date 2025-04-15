/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lock;

import org.junit.Test;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.tck.size.SmallTest;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SmallTest
public class URLPathLockTest {

  private LockFactory lockFactoryMock = mock(LockFactory.class);

  @Test
  public void testIsLocked() throws MalformedURLException {
    URL url = new URL("http://example.com");
    Lock mockLock = mock(Lock.class);
    when(mockLock.tryLock()).thenReturn(false);

    when(lockFactoryMock.createLock(url.toExternalForm())).thenReturn(mockLock);

    URLPathLock lock = new URLPathLock(url, lockFactoryMock);
    assertTrue(lock.isLocked());
  }

  @Test
  public void testGetPath() throws MalformedURLException {
    URL url = new URL("file:/");

    URLPathLock lock = new URLPathLock(url, lockFactoryMock);
    assertEquals(Paths.get("/"), lock.getPath());
  }

  @Test
  public void testGetPathException() throws MalformedURLException {
    URL wrongUrl = new URL("file:");

    URLPathLock lock = new URLPathLock(wrongUrl, lockFactoryMock);
    assertThrows(RuntimeException.class, () -> lock.getPath());
  }

  @Test
  public void testGetUriException() throws MalformedURLException {
    URL wrongUrl = new URL("file:");

    URLPathLock lock = new URLPathLock(wrongUrl, lockFactoryMock);
    assertThrows(RuntimeException.class, () -> lock.getUri());
  }


}
