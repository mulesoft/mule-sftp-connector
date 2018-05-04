/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import org.mule.runtime.api.i18n.I18nMessage;

public class FileBeingModifiedException extends SftpFileReadException {

  public FileBeingModifiedException(I18nMessage message) {
    super(message);
  }

  public FileBeingModifiedException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public FileBeingModifiedException(Throwable cause) {
    super(cause);
  }
}
