/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import org.mule.extension.file.common.api.AbstractFileAttributes;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.jcraft.jsch.SftpATTRS;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Metadata about a file in a SFTP server
 *
 * @since 1.0
 */
public class SftpFileAttributes extends AbstractFileAttributes {

  @Parameter
  private LocalDateTime timestamp;

  @Parameter
  private long size;

  @Parameter
  private boolean regularSize;

  @Parameter
  private boolean directory;

  @Parameter
  private boolean symbolicLink;

  /**
   * Creates a new instance
   *
   * @param path the file's {@link URI}
   * @param attrs the {@link SftpATTRS} which represents the file on the SFTP server
   */
  public SftpFileAttributes(URI path, SftpATTRS attrs) {
    super(path);

    Date timestamp = new Date(((long) attrs.getMTime()) * 1000L);
    this.timestamp = asDateTime(timestamp.toInstant());
    this.size = attrs.getSize();
    this.regularSize = attrs.isReg();
    this.directory = attrs.isDir();
    this.symbolicLink = attrs.isLink();
  }

  /**
   * @return The last time the file was modified
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRegularFile() {
    return regularSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectory() {
    return directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSymbolicLink() {
    return symbolicLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPath() {
    return normalizePath(super.getPath());
  }
}
