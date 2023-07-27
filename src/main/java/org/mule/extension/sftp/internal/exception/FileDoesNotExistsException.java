/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import static org.mule.extension.sftp.internal.error.FileError.FILE_DOESNT_EXIST;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which a given file does not exist or disappears while being read.
 *
 * @since 1.0
 */
public final class FileDoesNotExistsException extends ModuleException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public FileDoesNotExistsException(String message) {
    super(message, FILE_DOESNT_EXIST);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message   the detail message
   * @param exception cause of this exception
   */
  public FileDoesNotExistsException(String message, Exception exception) {
    super(message, FILE_DOESNT_EXIST, exception);
  }
}
