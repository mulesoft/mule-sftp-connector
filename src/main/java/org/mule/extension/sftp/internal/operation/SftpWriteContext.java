/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.api.WriteStrategy;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SftpWriteContext {

  private final Map<WriteStrategy, SftpWriteDelegateProvider> writeDelegateProviderMap = new HashMap<>();

  public SftpWriteContext(CustomWriteBufferSize customWriteBufferSize) {
    writeDelegateProviderMap.put(WriteStrategy.STANDARD, new SftpStandardWriteDelegateProvider());
    writeDelegateProviderMap.put(WriteStrategy.CUSTOM, new SftpCustomWriteDelegateProvider(customWriteBufferSize));
  }

  public void write(WriteStrategy writeStrategy, SftpFileSystemConnection fileSystem, String filePath, InputStream content,
                    FileWriteMode mode, boolean lock, boolean createParentDirectories) {
    SftpWriteDelegate sftpWriteDelegate = writeDelegateProviderMap.get(writeStrategy).getWriteDelegate();
    sftpWriteDelegate.write(fileSystem, filePath, content, mode, lock, createParentDirectories);
  }
}
