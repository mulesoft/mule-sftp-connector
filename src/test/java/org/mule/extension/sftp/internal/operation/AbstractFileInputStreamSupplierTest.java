/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.exception.DeletedFileWhileReadException;
import org.mule.extension.sftp.internal.exception.FileBeingModifiedException;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SmallTest
public class AbstractFileInputStreamSupplierTest {

  @Test
  void testDeletedFileWhileReadException() {
    AbstractFileInputStreamSupplier supplier = getMockSupplierWithNullAttributesImpl();
    assertThrows(DeletedFileWhileReadException.class, supplier::get);
  }

  @Test
  void testFileBeingModifiedException() {
    AbstractFileInputStreamSupplier supplier = getMockSupplierImpl();
    assertThrows(FileBeingModifiedException.class, supplier::get);
  }

  private static AbstractFileInputStreamSupplier getMockSupplierWithNullAttributesImpl() {
    return new AbstractFileInputStreamSupplier(mock(FileAttributes.class), 10L) {

      @Override
      protected FileAttributes getUpdatedAttributes() {
        return null;
      }

      @Override
      protected InputStream getContentInputStream() {
        return null;
      }
    };
  }

  private static AbstractFileInputStreamSupplier getMockSupplierImpl() {
    return new AbstractFileInputStreamSupplier(mock(FileAttributes.class), 10L) {

      long size = 0;

      @Override
      protected FileAttributes getUpdatedAttributes() {
        FileAttributes mockFileAttributes = mock(FileAttributes.class);
        when(mockFileAttributes.getSize()).thenReturn(++size);
        return mockFileAttributes;
      }

      @Override
      protected InputStream getContentInputStream() {
        return null;
      }
    };
  }
}
