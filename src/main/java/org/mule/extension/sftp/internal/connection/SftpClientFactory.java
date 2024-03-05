/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.runtime.api.scheduler.SchedulerService;

/**
 * Creates instances of {@link SftpClient}
 *
 * @since 1.0
 */
public class SftpClientFactory {

  /**
   * Creates a new instance which will connect to the given {@code host} and {@code port}
   *
   * @param host the host address
   * @param port the remote connection port
   * @param port the remote connection port
   * @return a {@link SftpClient}
   */
  public SftpClient createInstance(String host, int port, PRNGAlgorithm prngAlgorithm, SchedulerService schedulerService,
                                   SftpProxyConfig sftpProxyConfig, boolean kexHeader) {
    return new SftpClient(host, port, prngAlgorithm, schedulerService, kexHeader, sftpProxyConfig);
  }
}
