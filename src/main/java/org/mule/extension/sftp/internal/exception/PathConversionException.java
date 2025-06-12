/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import java.net.URISyntaxException;

/**
 * Exception thrown when there's an error converting between different path/URI representations.
 *
 * @since 1.0
 */
public class PathConversionException extends RuntimeException {

  /**
   * Creates a new instance with the given message and cause
   *
   * @param message the detail message
   * @param cause   the cause of the exception
   */
  public PathConversionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance wrapping a URISyntaxException
   *
   * @param cause the URISyntaxException that caused this error
   */
  public PathConversionException(URISyntaxException cause) {
    super("Failed to convert URL to URI", cause);
  }
}
