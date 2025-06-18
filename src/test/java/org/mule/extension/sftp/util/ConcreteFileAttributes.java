/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.util;

import org.mule.extension.sftp.api.AbstractFileAttributes;

import java.net.URI;
import java.nio.file.Path;

public class ConcreteFileAttributes extends AbstractFileAttributes {

  public ConcreteFileAttributes(Path path) {
    super(path);
  }

  public ConcreteFileAttributes(URI uri) {
    super(uri);
  }

  @Override
  public long getSize() {
    return 0;
  }

  @Override
  public boolean isRegularFile() {
    return false;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean isSymbolicLink() {
    return false;
  }
}
