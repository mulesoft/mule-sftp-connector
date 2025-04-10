/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.Test;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.error.FileError;
import org.mule.extension.sftp.internal.exception.*;
import org.mule.extension.sftp.internal.operation.AbstractFileInputStreamSupplier;
import org.mule.extension.sftp.internal.util.UriUtils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

@SmallTest
public class ExceptionsTestCase {

  @Test(expected = IllegalPathException.class)
  public void testIllegalPathException() {
    UriUtils.createUri(":Illegal path with colon", "");
  }

  @Test(expected = SftpConnectionException.class)
  public void testSftpConnectionException() throws ConnectionException {
    SftpClient client = new SftpClient("", 0, PRNGAlgorithm.SHA1PRNG, null);
    client.setProxyConfig(new SftpProxyConfig());
  }

  @Test(expected = DeletedFileWhileReadException.class)
  public void testDeletedFileWhileReadException() {
    AbstractFileInputStreamSupplier supplier = new AbstractFileInputStreamSupplier(mock(FileAttributes.class), 10L) {

      @Override
      protected FileAttributes getUpdatedAttributes() {
        return null;
      }

      @Override
      protected InputStream getContentInputStream() {
        return null;
      }
    };
    supplier.get();
  }

  @Test(expected = FileBeingModifiedException.class)
  public void testFileBeingModifiedException() {
    AbstractFileInputStreamSupplier supplier = new AbstractFileInputStreamSupplier(mock(FileAttributes.class), 10L) {

      @Override
      protected FileAttributes getUpdatedAttributes() {
        final int[] size = {0};
        return new FileAttributes() {

          @Override
          public long getSize() {
            return ++size[0];
          }

          @Override
          public boolean isRegularFile() {
            return false;
          }

          @Override
          public boolean isDirectory() {
            return false;
          }

          @Override
          public boolean isSymbolicLink() {
            return false;
          }

          @Override
          public String getPath() {
            return null;
          }

          @Override
          public String getName() {
            return null;
          }
        };
      }

      @Override
      protected InputStream getContentInputStream() {
        return null;
      }
    };
    supplier.get();
  }
}
