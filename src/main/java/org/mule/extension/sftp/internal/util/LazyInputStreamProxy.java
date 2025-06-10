/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import org.mule.extension.sftp.internal.stream.LazyStreamSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class LazyInputStreamProxy extends InputStream {

  private final LazyStreamSupplier streamSupplier;
  private final AtomicReference<InputStream> delegateRef = new AtomicReference<>();



  public LazyInputStreamProxy(LazyStreamSupplier streamSupplier) {
    this.streamSupplier = streamSupplier;
  }

  private InputStream getDelegate() {
    InputStream local = delegateRef.get();
    if (local == null) {
      InputStream created = streamSupplier.get();
      if (delegateRef.compareAndSet(null, created)) {
        return created;
      }
      local = delegateRef.get();
    }
    return local;
  }

  @Override
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return getDelegate().read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return getDelegate().read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return getDelegate().skip(n);
  }

  @Override
  public int available() throws IOException {
    return getDelegate().available();
  }

  @Override
  public void close() throws IOException {
    InputStream is = getDelegate();
    if (is != null) {
      is.close();
    }
  }

  @Override
  public synchronized void mark(int readlimit) {
    getDelegate().mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    getDelegate().reset();
  }

  @Override
  public boolean markSupported() {
    return getDelegate().markSupported();
  }
}

