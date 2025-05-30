/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

/**
 * List different strategies regarding how to write new files
 *
 * @since 1.0
 */
public enum FileWriteMode {
  /**
   * Means that if the file to be written already exists, then it should be overwritten
   */
  OVERWRITE,

  /**
   * Means that if the file to be written already exists, then the content should be appended to that file
   */
  APPEND,

  /**
   * Means that if the file to be written using CUSTOM WRITE strategy already exists, then the content should be appended to that file
   */
  CUSTOM_APPEND,

  /**
  * Means that a new file should be created and an error should be raised if the file already exists
  */
  CREATE_NEW
}
