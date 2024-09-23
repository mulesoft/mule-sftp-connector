/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

/**
 * List different options regarding how to write new files
 *
 * @since 2.3
 */
public enum WriteStrategy {
  /**
   * Means that the standard write function is used where the file write mode is honoured
   */
  STANDARD,
  /**
   * Means that a custom write function is used to write to files where offset is manually calculated
   */
  CUSTOM
}
