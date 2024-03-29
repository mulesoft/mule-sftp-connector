/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static org.mule.extension.sftp.internal.error.FileError.DISCONNECTED;
import static org.mule.extension.sftp.internal.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.util.SftpUtils.normalizePath;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.operation.CopyCommand;
import org.mule.extension.sftp.internal.operation.CreateDirectoryCommand;
import org.mule.extension.sftp.internal.operation.DeleteCommand;
import org.mule.extension.sftp.internal.operation.MoveCommand;
import org.mule.extension.sftp.internal.operation.RenameCommand;
import org.mule.extension.sftp.internal.operation.SftpCopyCommand;
import org.mule.extension.sftp.internal.operation.SftpCreateDirectoryCommand;
import org.mule.extension.sftp.internal.operation.SftpDeleteCommand;
import org.mule.extension.sftp.internal.operation.SftpListCommand;
import org.mule.extension.sftp.internal.operation.SftpMoveCommand;
import org.mule.extension.sftp.internal.operation.SftpReadCommand;
import org.mule.extension.sftp.internal.operation.SftpRenameCommand;
import org.mule.extension.sftp.internal.operation.SftpWriteCommand;
import org.mule.extension.sftp.internal.operation.WriteCommand;
import org.mule.extension.sftp.internal.lock.URLPathLock;
import org.mule.extension.sftp.internal.lock.UriLock;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AbstractFileSystem} for files residing on a SFTP server
 *
 * @since 1.0
 */
public class SftpFileSystemConnection extends AbstractExternalFileSystem {

  public static final String ROOT = "/";
  public static final Logger LOGGER = LoggerFactory.getLogger(SftpFileSystemConnection.class);

  private static String resolveBasePath(String basePath) {
    if (isBlank(basePath)) {
      return "";
    }
    return createUri(ROOT, basePath).getPath();
  }

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

  public SftpFileSystemConnection(SftpClient client, String basePath, LockFactory lockFactory) {
    super(resolveBasePath(basePath));
    this.client = client;
    this.lockFactory = lockFactory;

    copyCommand = new SftpCopyCommand(this, client);
    createDirectoryCommand = new SftpCreateDirectoryCommand(this, client);
    deleteCommand = new SftpDeleteCommand(this, client);
    moveCommand = new SftpMoveCommand(this, client);
    readCommand = new SftpReadCommand(this, client);
    listCommand = new SftpListCommand(this, client);
    renameCommand = new SftpRenameCommand(this, client);
    writeCommand = new SftpWriteCommand(this, client);
    client.setOwner(this);
  }

  public void disconnect() {
    client.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeToBaseDir() {
    if (!isBlank(getBasePath())) {
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
   */
  protected UriLock createLock(URI uri) {
    return new URLPathLock(toURL(uri), lockFactory);
  }

  private URL toURL(URI uri) {
    try {
      return new URL("ftp", client.getHost(), client.getPort(), uri != null ? uri.getPath() : EMPTY);
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
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Connection validation failed.");
      }
      return failure("Connection is stale", new SftpConnectionException("Connection is stale", DISCONNECTED));
    }
    try {
      changeToBaseDir();
    } catch (Exception e) {
      LOGGER.error("Error occurred while changing to base directory {}", getBasePath(), e);
      return failure("Configured workingDir is unavailable", e);
    }
    return success();
  }

  public SftpClient getClient() {
    return client;
  }
}
