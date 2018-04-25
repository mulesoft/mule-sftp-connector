/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.sftp.internal.connection.SftpConnectionProvider;
import org.mule.extension.sftp.internal.source.SftpDirectoryListener;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.inject.Inject;

/**
 * Allows manipulating files through the FTP and SFTP
 *
 * @since 1.0
 */
@Extension(name = "SFTP")
@Operations({SftpOperations.class})
@ConnectionProviders({SftpConnectionProvider.class})
@ErrorTypes(FileError.class)
@Sources(SftpDirectoryListener.class)
@Xml(prefix = "sftp")
public class SftpConnector extends FileConnectorConfig {

  @Inject
  private ConnectionManager connectionManager;

  /**
   * Wait time in milliseconds between size checks to determine if a file is ready to be processed. This allows a file write to
   * complete before processing. You can disable this feature by setting to a negative number or omitting a value. When enabled,
   * Mule performs two size checks waiting the specified time between calls. If both checks return the same value, the file is
   * ready to process.
   */
  @Parameter
  @Optional(defaultValue = "-1")
  private long sizeCheckWaitTime;


  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }
}
