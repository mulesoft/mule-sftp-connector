/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.extension;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import org.mule.extension.sftp.internal.operation.SftpOperations;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.error.FileError;
import org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider;
import org.mule.extension.sftp.internal.source.SftpDirectorySource;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.JavaVersionSupport;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Allows manipulating files through the FTP and SFTP
 *
 * @since 1.0
 */
@Extension(name = "SFTP", category = COMMUNITY)
@Operations({SftpOperations.class})
@ConnectionProviders({SftpConnectionProvider.class})
@ErrorTypes(FileError.class)
@Sources(SftpDirectorySource.class)
@Xml(prefix = "sftp")
@JavaVersionSupport({JAVA_8, JAVA_11})
public class SftpConnector extends FileConnectorConfig {

  @Inject
  private ConnectionManager connectionManager;

  /**
   * Wait time between size checks to determine if a file is ready to be read. This allows a file write to complete before
   * processing. If no value is provided, the check will not be performed. When enabled, Mule performs two size checks waiting the
   * specified time between calls. If both checks return the same value, the file is ready to be read.This attribute works in
   * tandem with {@link #timeBetweenSizeCheckUnit}.
   */
  @Parameter
  @Placement(tab = ADVANCED_TAB)
  @Summary("Wait time between size checks to determine if a file is ready to be read.")
  @Optional
  private Long timeBetweenSizeCheck;

  /**
   * A {@link TimeUnit} which qualifies the {@link #timeBetweenSizeCheck} attribute.
   * <p>
   * Defaults to {@code MILLISECONDS}
   */
  @Parameter
  @Placement(tab = ADVANCED_TAB)
  @Optional(defaultValue = "MILLISECONDS")
  @Summary("Time unit to be used in the wait time between size checks")
  private TimeUnit timeBetweenSizeCheckUnit;

  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }
}
