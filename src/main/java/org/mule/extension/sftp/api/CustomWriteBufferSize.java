/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

/**
 * List different size options of the buffer for customWrite
 *
 * @since 2.3
 */
public enum CustomWriteBufferSize {
  /**
   * The buffer size options for custom write
   */
  BUFFER_SIZE_1KB(1024), BUFFER_SIZE_2KB(2048), BUFFER_SIZE_4KB(4096), BUFFER_SIZE_8KB(8192), BUFFER_SIZE_16KB(16384);

  private final int customWriteBufferSize;

  CustomWriteBufferSize(int customWriteBufferSize) {
    this.customWriteBufferSize = customWriteBufferSize;
  }

  public int getCustomWriteBufferSize() {
    return customWriteBufferSize;
  }
}
