/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;


import org.slf4j.LoggerFactory;

public class SftpLogger  {
  private org.slf4j.Logger logger = LoggerFactory.getLogger("com.jcraft.jsch");

  public boolean isEnabled(int level) {
    switch (level) {
      case 0:
        return this.logger.isDebugEnabled();
      case 1:
        return this.logger.isInfoEnabled();
      case 2:
        return this.logger.isWarnEnabled();
      case 3:
        return this.logger.isErrorEnabled();
      case 4:
        return this.logger.isErrorEnabled();
      default:
        return false;
    }
  }


  public void log(int level, String message) {
    this.logger.debug(message);
  }
}
