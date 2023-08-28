/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import org.mule.extension.sftp.internal.operation.SftpCommand;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import java.net.URI;

/**
 * A {@link SftpCommand} which implements support functionality for {@link SftpDirectorySource}
 *
 * @since 1.1
 */
public class OnNewFileCommand extends SftpCommand {

  OnNewFileCommand(SftpFileSystemConnection fileSystem) {
    super(fileSystem);
  }

  /**
   * Resolves the root path on which the listener needs to be created
   *
   * @param directory the path that the user configured on the listener
   * @return the resolved {@link URI} to listen on
   */
  public URI resolveRootPath(String directory) {
    return resolveExistingPath(directory);
  }
}
