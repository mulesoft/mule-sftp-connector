/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lock;

import org.mule.extension.sftp.internal.exception.PathConversionException;
import org.mule.runtime.api.lock.LockFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

/**
 * A {@link PathLock} which is based on {@link Lock locks} obtained through a {@link #lockFactory}. The lock's keys are generated
 * through the external form of a {@link URL}
 *
 * @since 1.0
 */
public class URLPathLock implements PathLock, UriLock {

  private final URL url;
  private final LockFactory lockFactory;
  private final AtomicReference<Lock> ownedLock = new AtomicReference<>();

  /**
   * Creates a new instance
   *
   * @param url         the URL from which the lock's key is to be extracted
   * @param lockFactory a {@link LockFactory}
   */
  public URLPathLock(URL url, LockFactory lockFactory) {
    this.url = url;
    this.lockFactory = lockFactory;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("java:S2222")
  @Override
  public boolean tryLock() {
    Lock lock = getLock();
    if (lock.tryLock()) {
      ownedLock.set(lock);
      return true;
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLocked() {
    if (ownedLock.get() != null) {
      return true;
    }

    Lock lock = getLock();
    try {
      return !lock.tryLock();
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("java:S2235")
  @Override
  public void release() {
    Lock lock = ownedLock.getAndSet(null);
    if (lock != null) {
      try {
        lock.unlock();
      } catch (IllegalMonitorStateException e) {
        // ignore
      }
    }
  }

  private Lock getLock() {
    return lockFactory.createLock(url.toExternalForm());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getPath() {
    try {
      return Paths.get(url.toURI());
    } catch (URISyntaxException e) {
      throw new PathConversionException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI getUri() {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new PathConversionException(e);
    }
  }

}
