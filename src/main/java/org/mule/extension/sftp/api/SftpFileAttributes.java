/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import static org.mule.extension.sftp.internal.util.SftpUtils.asDateTime;
import static org.mule.extension.sftp.internal.util.SftpUtils.normalizePath;

import static org.apache.sshd.sftp.client.SftpClient.Attributes;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.net.URI;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


/**
 * Metadata about a file in a SFTP server
 *
 * @since 1.0
 */
public class SftpFileAttributes extends AbstractFileAttributes {

  private static final long serialVersionUID = 686949251083311882L;

  @Parameter
  private ZonedDateTime timestamp;

  @Parameter
  private long size;

  @Parameter
  private boolean regularFile;

  @Parameter
  private boolean directory;

  @Parameter
  private boolean symbolicLink;

  /**
   * Creates a new instance (Default constructor)
   */
  public SftpFileAttributes() {
    super();
  }

  /**
   * Creates a new instance
   *
   * @param uri   the file's {@link URI}
   * @param attrs the {@link Attributes} which represents the file on the SFTP server
   */
  public SftpFileAttributes(URI uri, Attributes attrs) {
    super(uri);

    this.timestamp = asDateTime(attrs.getModifyTime().toInstant());
    this.size = attrs.getSize();
    this.regularFile = attrs.isRegularFile();
    this.directory = attrs.isDirectory();
    this.symbolicLink = attrs.isSymbolicLink();
  }

  /**
   * @return The last time the file was modified
   */
  public ZonedDateTime getTimestamp() {
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
    return regularFile;
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (!(o instanceof SftpFileAttributes))
      return false;

    SftpFileAttributes that = (SftpFileAttributes) o;

    return new EqualsBuilder().append(getSize(), that.getSize()).append(regularFile, that.regularFile)
        .append(isDirectory(), that.isDirectory()).append(isSymbolicLink(), that.isSymbolicLink())
        .append(getTimestamp(), that.getTimestamp()).append(getPath(), that.getPath()).append(getName(), that.getName())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getTimestamp()).append(getSize()).append(regularFile).append(isDirectory())
        .append(isSymbolicLink()).append(getPath()).append(getName()).toHashCode();
  }

}
