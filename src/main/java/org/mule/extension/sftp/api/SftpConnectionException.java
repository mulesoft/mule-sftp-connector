/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.sftp.internal.SftpConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ConnectionException} implementation to declare connectivity errors in the {@link SftpConnector}
 *
 * @since 1.0
 */
public class SftpConnectionException extends ConnectionException {

  public SftpConnectionException(String s) {
    super(s);
  }

  public SftpConnectionException(String message, FileError errors) {
    super(message, new ModuleException(message, errors));
  }

  public SftpConnectionException(Throwable throwable, FileError fileError) {
    super(new ModuleException(fileError, throwable));
  }

  public SftpConnectionException(String message, Throwable throwable, FileError fileError) {
    super(message, new ModuleException(fileError, throwable));
  }

  public SftpConnectionException(String message, Throwable throwable, FileError fileError, Object connection) {
    super(message, new ModuleException(fileError, throwable), null, connection);
  }
}
