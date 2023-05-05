/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

/**
 * A {@link SftpCommand} which implements the {@link RenameCommand} contract
 *
 * @since 1.0
 */
public final class SftpRenameCommand extends SftpCommand implements RenameCommand {

  /**
   * {@inheritDoc}
   */
  public SftpRenameCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rename(String filePath, String newName, boolean overwrite) {
    super.rename(filePath, newName, overwrite);
  }
}
