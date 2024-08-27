/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.CustomWriteBufferSize;

public class SftpCustomWriteDelegateProvider implements SftpWriteDelegateProvider {

  private final CustomWriteBufferSize customWriteBufferSize;

  public SftpCustomWriteDelegateProvider(CustomWriteBufferSize customWriteBufferSize) {
    this.customWriteBufferSize = customWriteBufferSize;
  }

  /**
   * @return
   */
  @Override
  public SftpWriteDelegate getWriteDelegate() {
    return new SftpCustomWriteDelegate(customWriteBufferSize);
  }
}
