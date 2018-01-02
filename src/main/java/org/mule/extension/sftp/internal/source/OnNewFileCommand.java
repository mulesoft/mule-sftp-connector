/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.mule.extension.sftp.internal.command.SftpCommand;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.nio.file.Path;

/**
 * A {@link SftpCommand} which implements support functionality for {@link SftpDirectoryListener}
 *
 * @since 1.1
 */
public class OnNewFileCommand extends SftpCommand {

  OnNewFileCommand(SftpFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * Resolves the root path on which the listener needs to be created
   *
   * @param directory the path that the user configured on the listener
   * @return the resolved {@link Path} to listen on
   */
  public Path resolveRootPath(String directory) {
    return resolveExistingPath(directory);
  }
}
