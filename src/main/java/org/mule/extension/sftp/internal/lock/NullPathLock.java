/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lock;

import java.nio.file.Path;

/**
 * Implementation of the Null Object design pattern for the {@link PathLock} interface
 *
 * @since 1.0
 */
public final class NullPathLock extends AbstractNullLock implements PathLock {

  private final Path path;

  public NullPathLock(Path path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  public Path getPath() {
    return path;
  }

}
