/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.stream.LazyStreamSupplier;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SmallTest
public class LazyInputStreamProxyTestCase {

  @Test
  void testLazyInputStreamProxyOverridenMethods() throws IOException {
    LazyStreamSupplier supplier = new LazyStreamSupplier(() -> new ByteArrayInputStream("test".getBytes()));
    LazyInputStreamProxy lazyInputStreamProxy = new LazyInputStreamProxy(supplier);

    assertEquals(4, lazyInputStreamProxy.skip(9L));
    assertEquals(-1, lazyInputStreamProxy.read());
    lazyInputStreamProxy.mark(1);
    assertEquals(true, lazyInputStreamProxy.markSupported());
    lazyInputStreamProxy.reset();
  }
}
