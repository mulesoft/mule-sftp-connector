/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.Test;
import org.mule.extension.sftp.internal.connection.TimeoutSettings;
import org.mule.extension.sftp.internal.error.FileError;
import org.mule.extension.sftp.internal.exception.DeletedFileWhileReadException;
import org.mule.extension.sftp.internal.exception.FileBeingModifiedException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

@SmallTest
public class ExceptionsTestCase {

  @Test
  public void testExceptions() {

    new DeletedFileWhileReadException(createStaticMessage("DeletedFileWhileReadException."));
    new FileBeingModifiedException(createStaticMessage("FileBeingModifiedException."));
    new IllegalPathException("IllegalPathException.", new Exception());
    new SftpConnectionException("SftpConnectionException.", FileError.UNKNOWN);
  }
}
