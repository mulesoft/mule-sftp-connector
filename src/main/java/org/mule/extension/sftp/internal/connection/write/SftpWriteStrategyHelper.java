/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.WriteStrategy;

/**
 * A strategy helper class to pick the WriteStrategy
 *
 * @since 2.3
 */
public class SftpWriteStrategyHelper {

  /**
   * Gets the strategy for the write method
   *
   * @param muleSftpClient              the SftpClient Instance for writing to the file
   * @param apacheSftpClient            the Apache client instance for writing to the file
   * @param writeStrategy               a {@link WriteStrategy}. Defaults to STANDARD
   * @param bufferSizeForWriteStrategy  a {@link CustomWriteBufferSize}. Defaults to 8192
   */
  public static SftpWriter getStrategy(org.mule.extension.sftp.internal.connection.SftpClient muleSftpClient,
                                       org.apache.sshd.sftp.client.SftpClient apacheSftpClient,
                                       WriteStrategy writeStrategy, CustomWriteBufferSize bufferSizeForWriteStrategy) {
    switch (writeStrategy) {
      case CUSTOM:
        return new SftpCustomWriter(muleSftpClient, apacheSftpClient, bufferSizeForWriteStrategy);
      default: // STANDARD
        return new SftpStandardWriter(muleSftpClient);
    }
  }
}
