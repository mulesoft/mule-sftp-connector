/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 * This exception is thrown when at the moment of getting the actual content of a file, it has already been deleted.
 *
 * @since 1.2.0
 */
@SuppressWarnings("java:S110")
public class DeletedFileWhileReadException extends FileReadException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public DeletedFileWhileReadException(I18nMessage message) {
    super(message);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   * @param cause
   */
  public DeletedFileWhileReadException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}
