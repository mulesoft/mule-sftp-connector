/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.error.provider;

import static org.mule.extension.sftp.internal.error.FileError.ACCESS_DENIED;
import static org.mule.extension.sftp.internal.error.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.sftp.internal.error.FileError.FILE_DOESNT_EXIST;
import static org.mule.extension.sftp.internal.error.FileError.FILE_LOCK;
import static org.mule.extension.sftp.internal.error.FileError.ILLEGAL_CONTENT;
import static org.mule.extension.sftp.internal.error.FileError.ILLEGAL_PATH;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.operation.BaseFileSystemOperations;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Errors that can be thrown in the
 * {@link BaseFileSystemOperations#doWrite(FileConnectorConfig, FileSystem, String, InputStream, String, boolean, boolean, FileWriteMode)}
 * operation.
 *
 * @since 1.0
 */
public class FileWriteErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return unmodifiableSet(new HashSet<>(asList(ILLEGAL_PATH, ILLEGAL_CONTENT, FILE_ALREADY_EXISTS,
                                                ACCESS_DENIED, FILE_LOCK, FILE_DOESNT_EXIST)));
  }
}
