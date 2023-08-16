/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Base class for implementations of {@link org.mule.extension.sftp.api.FileAttributes}
 *
 * @since 1.0
 */
public abstract class AbstractFileAttributes implements FileAttributes, Serializable {

  private static final long serialVersionUID = 3249780732227598L;

  @Parameter
  protected String path;

  @Parameter
  private String fileName;

  /**
   * Creates a new instance
   *
   * @param path a {@link Path} pointing to the represented file
   */
  protected AbstractFileAttributes(Path path) {
    this.path = path.toString();
    this.fileName = path.getFileName() != null ? path.getFileName().toString() : "";
  }

  /**
   * Default contructor (Creates a new instance)
   */
  protected AbstractFileAttributes() {}

  /**
   * Creates a new instance
   *
   * @param uri a {@link URI} pointing to the represented file
   */
  protected AbstractFileAttributes(URI uri) {
    this.path = uri.getPath();
    String name = FilenameUtils.getName(uri.getPath());
    this.fileName = name != null ? name : "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return fileName;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof AbstractFileAttributes)) return false;

    AbstractFileAttributes that = (AbstractFileAttributes) o;

    return new EqualsBuilder().append(getPath(), that.getPath()).append(fileName, that.fileName).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getPath()).append(fileName).toHashCode();
  }
}
