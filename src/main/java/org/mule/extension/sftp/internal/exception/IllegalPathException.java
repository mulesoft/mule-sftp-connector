/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import static org.mule.extension.sftp.internal.error.FileError.ILLEGAL_PATH;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which a given path is invalid. For instance, if the path is {@code null}
 * or doesn't exist.
 *
 * @since 1.0
 */
@SuppressWarnings("java:S110")
public final class IllegalPathException extends ModuleException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public IllegalPathException(String message) {
    super(message, ILLEGAL_PATH);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message   the detail message
   * @param exception cause of this exception
   */
  public IllegalPathException(String message, Exception exception) {
    super(message, ILLEGAL_PATH, exception);
  }
}
