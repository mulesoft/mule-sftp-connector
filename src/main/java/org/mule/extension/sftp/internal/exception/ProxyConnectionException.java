/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;


/**
 * Exception thrown when there's an error converting between different path/URI representations.
 *
 * @since 1.0
 */
public class ProxyConnectionException extends Exception {

  /**
   * Creates a new instance with the given message
   *
   * @param message the detail message
   */
  public ProxyConnectionException(String message) {
    super(message);
  }

  /**
   * Creates a new instance with the given message and cause
   *
   * @param message the detail message
   * @param cause   the cause of the exception
   */
  public ProxyConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance wrapping another exception
   *
   * @param cause the exception that caused this error
   */
  public ProxyConnectionException(Throwable cause) {
    super(cause);
  }
}
