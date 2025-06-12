/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import static org.mule.extension.sftp.internal.error.FileError.ACCESS_DENIED;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which there is not enough permissions to access a particular file or
 * directory.
 *
 * @since 1.0
 */
@SuppressWarnings("java:S110")
public final class FileAccessDeniedException extends ModuleException {

  public FileAccessDeniedException(String message, Exception exception) {
    super(message, ACCESS_DENIED, exception);
  }

  public FileAccessDeniedException(String message) {
    super(message, ACCESS_DENIED);
  }
}
