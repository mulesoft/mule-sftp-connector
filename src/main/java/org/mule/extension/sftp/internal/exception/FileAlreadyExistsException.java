/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import static org.mule.extension.sftp.internal.error.FileError.FILE_ALREADY_EXISTS;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which a given file already exists. For example, when trying to create a
 * new file with a {@link FileWriteMode#CREATE_NEW} write mode and the file already existed.
 *
 * @since 1.0
 */
@SuppressWarnings("java:S110")
public final class FileAlreadyExistsException extends ModuleException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public FileAlreadyExistsException(String message) {
    super(message, FILE_ALREADY_EXISTS);
  }
}
