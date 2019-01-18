/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.extension.file.common.api.exceptions.FileError.DISCONNECTED;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.extension.file.common.api.AbstractFileSystem;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.command.CopyCommand;
import org.mule.extension.file.common.api.command.CreateDirectoryCommand;
import org.mule.extension.file.common.api.command.DeleteCommand;
import org.mule.extension.file.common.api.command.MoveCommand;
import org.mule.extension.file.common.api.command.RenameCommand;
import org.mule.extension.file.common.api.command.WriteCommand;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.file.common.api.lock.URLPathLock;
import org.mule.extension.sftp.api.SftpConnectionException;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.command.SftpCopyCommand;
import org.mule.extension.sftp.internal.command.SftpCreateDirectoryCommand;
import org.mule.extension.sftp.internal.command.SftpDeleteCommand;
import org.mule.extension.sftp.internal.command.SftpListCommand;
import org.mule.extension.sftp.internal.command.SftpMoveCommand;
import org.mule.extension.sftp.internal.command.SftpReadCommand;
import org.mule.extension.sftp.internal.command.SftpRenameCommand;
import org.mule.extension.sftp.internal.command.SftpWriteCommand;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Implementation of {@link AbstractFileSystem} for files residing on a SFTP server
 *
 * @since 1.0
 */
public class SftpFileSystem extends AbstractFileSystem {

  protected final SftpClient client;
  protected final CopyCommand copyCommand;
  protected final CreateDirectoryCommand createDirectoryCommand;
  protected final DeleteCommand deleteCommand;
  protected final SftpListCommand listCommand;
  protected final MoveCommand moveCommand;
  protected final SftpReadCommand readCommand;
  protected final RenameCommand renameCommand;
  protected final WriteCommand writeCommand;
  private final LockFactory lockFactory;


  private static String resolveBasePath(String basePath, SftpClient client) {
    if (isBlank(basePath)) {
      try {
        return client.getWorkingDirectory();
      } catch (Exception e) {
        throw new IllegalPathException("SFTP working dir was not specified and failed to resolve a default one",
                                       e);
      }
    }
    return basePath;
  }

  public SftpFileSystem(SftpClient client, String basePath, LockFactory lockFactory) {
    super(resolveBasePath(basePath, client));
    this.client = client;
    this.lockFactory = lockFactory;

    copyCommand = new SftpCopyCommand(this, client);
    createDirectoryCommand = new SftpCreateDirectoryCommand(this, client);
    deleteCommand = new SftpDeleteCommand(this, client);
    moveCommand = new SftpMoveCommand(this, client);
    readCommand = new SftpReadCommand(this, client);
    listCommand = new SftpListCommand(this, client, (SftpReadCommand) readCommand);
    renameCommand = new SftpRenameCommand(this, client);
    writeCommand = new SftpWriteCommand(this, client);
  }

  public void disconnect() {
    client.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeToBaseDir() {
    if (getBasePath() != null) {
      client.changeWorkingDirectory(getBasePath());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getBasePath() {
    return normalizePath(super.getBasePath());
  }

  public InputStream retrieveFileContent(FileAttributes filePayload) {
    return client.getFileContent(filePayload.getPath());
  }

  public SftpFileAttributes readFileAttributes(String filePath) {
    return getReadCommand().readAttributes(filePath);
  }

  protected boolean isConnected() {
    return client.isConnected();
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@link URLPathLock} based on the {@link #client}'s connection information
   */
  @Override
  protected PathLock createLock(Path path) {
    return new URLPathLock(toURL(path), lockFactory);
  }

  private URL toURL(Path path) {
    try {
      return new URL("ftp", client.getHost(), client.getPort(), path != null ? path.toString() : EMPTY);
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not get URL for SFTP server"), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CopyCommand getCopyCommand() {
    return copyCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CreateDirectoryCommand getCreateDirectoryCommand() {
    return createDirectoryCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteCommand getDeleteCommand() {
    return deleteCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SftpListCommand getListCommand() {
    return listCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MoveCommand getMoveCommand() {
    return moveCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SftpReadCommand getReadCommand() {
    return readCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RenameCommand getRenameCommand() {
    return renameCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteCommand getWriteCommand() {
    return writeCommand;
  }

  /**
   * Validates the underlying connection to the remote server
   *
   * @return a {@link ConnectionValidationResult}
   */
  public ConnectionValidationResult validateConnection() {
    if (!isConnected()) {
      return failure("Connection is stale", new SftpConnectionException("Connection is stale", DISCONNECTED));
    }

    try {
      changeToBaseDir();
    } catch (Exception e) {
      failure("Configured workingDir is unavailable", e);
    }
    return success();
  }

  public SftpClient getClient() {
    return client;
  }
}
