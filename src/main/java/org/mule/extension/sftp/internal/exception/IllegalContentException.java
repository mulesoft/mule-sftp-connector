/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import static org.mule.extension.sftp.internal.error.FileError.ILLEGAL_CONTENT;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which the received content to be written is invalid.
 *
 * @since 1.0
 */
@SuppressWarnings("java:S110")
public final class IllegalContentException extends ModuleException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public IllegalContentException(String message) {
    super(message, ILLEGAL_CONTENT);
  }
}
