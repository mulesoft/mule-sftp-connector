/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.matcher;

import org.mule.extension.sftp.api.FileAttributes;

import java.util.function.Predicate;

/**
 * A {@link Predicate} of {@link FileAttributes} instances which accepts any value
 *
 * @since 1.0
 */
public final class NullFilePayloadPredicate<T extends FileAttributes> implements Predicate<T> {

  /**
   * @return {@code true}
   */
  @Override
  public boolean test(T fileAttributes) {
    return true;
  }
}
