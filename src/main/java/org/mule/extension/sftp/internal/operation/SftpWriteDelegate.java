/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import java.io.InputStream;

/**
 * @param fileSystem              a reference to the host {@link FileSystem}
 * @param filePath                the path of the file to be written
 * @param content                 the content to be written into the file
 * @param mode                    a {@link FileWriteMode}
 * @param lock                    whether or not to lock the file
 * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
 * @throws IllegalArgumentException   if an illegal combination of arguments is supplied
 */
public interface SftpWriteDelegate {

  void write(SftpFileSystemConnection fileSystem, String filePath, InputStream content, FileWriteMode mode,
             boolean lock, boolean createParentDirectories);
}
