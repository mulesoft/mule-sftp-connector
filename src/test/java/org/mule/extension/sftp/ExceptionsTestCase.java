/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.Test;
import org.mule.extension.sftp.internal.error.FileError;
import org.mule.extension.sftp.internal.exception.*;
import org.mule.tck.size.SmallTest;


import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

@SmallTest
public class ExceptionsTestCase {

  @Test
  public void testExceptions() {

    /* DeletedFileWhileReadException class */
    DeletedFileWhileReadException deletedFileWhileReadException =
        new DeletedFileWhileReadException(createStaticMessage("DeletedFileWhileReadException."));
    assertEquals("DeletedFileWhileReadException.", deletedFileWhileReadException.getMessage());

    DeletedFileWhileReadException deletedFileWhileReadException2 =
        new DeletedFileWhileReadException(new Throwable());
    assertEquals("java.lang.Throwable", deletedFileWhileReadException2.getMessage());


    /* FileAccessDeniedException class */
    FileAccessDeniedException fileAccessDeniedException =
        new FileAccessDeniedException("FileAccessDeniedException.");
    assertEquals("FileAccessDeniedException.", fileAccessDeniedException.getMessage());


    /* FileAlreadyExistsException class */
    FileAlreadyExistsException fileAlreadyExistsException =
        new FileAlreadyExistsException("FileAlreadyExistsException.", new Exception());
    assertEquals("FileAlreadyExistsException.", fileAlreadyExistsException.getMessage());


    /* FileBeingModifiedException class */
    FileBeingModifiedException fileBeingModifiedException =
        new FileBeingModifiedException(createStaticMessage("FileBeingModifiedException."), new Throwable());
    assertEquals("FileBeingModifiedException.", fileBeingModifiedException.getMessage());

    FileBeingModifiedException fileBeingModifiedException2 =
        new FileBeingModifiedException(new Throwable());
    assertEquals("java.lang.Throwable", fileBeingModifiedException2.getMessage());

    FileBeingModifiedException fileBeingModifiedException3 =
        new FileBeingModifiedException(createStaticMessage("FileBeingModifiedException."));
    assertEquals("FileBeingModifiedException.", fileBeingModifiedException3.getMessage());


    /* FileDoesNotExistsException class */
    FileDoesNotExistsException fileDoesNotExistsException =
        new FileDoesNotExistsException("FileDoesNotExistsException.");
    assertEquals("FileDoesNotExistsException.", fileDoesNotExistsException.getMessage());


    /* IllegalPathException class */
    IllegalPathException illegalPathException =
        new IllegalPathException("IllegalPathException.", new Exception());
    assertEquals("IllegalPathException.", illegalPathException.getMessage());


    /* SftpConnectionException class */
    SftpConnectionException sftpConnectionException =
        new SftpConnectionException("SftpConnectionException.", FileError.UNKNOWN);
    assertEquals("SftpConnectionException.", sftpConnectionException.getMessage());

    SftpConnectionException sftpConnectionException2 =
        new SftpConnectionException("SftpConnectionException.");
    assertEquals("SftpConnectionException.", sftpConnectionException2.getMessage());

    SftpConnectionException sftpConnectionException3 =
        new SftpConnectionException(new Throwable(), FileError.UNKNOWN);
    assertEquals("java.lang.Throwable", sftpConnectionException3.getMessage());

  }
}
