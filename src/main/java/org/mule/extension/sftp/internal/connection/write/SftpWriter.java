/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.FileWriteMode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * This interface acts as a facade which allows implementing the different write strategies to be used for writing to a file.
 *
 * @since 2.3.0
 */
public interface SftpWriter {

  /**
   * Writes to a file
   *
   * @param path                        the path of the file to be written
   * @param stream                      the content to be written into the file
   * @param mode                        a {@link FileWriteMode}
   * @param uri                         the URI of the file to be written
   */
  void write(String path, InputStream stream, FileWriteMode mode, URI uri) throws IOException;

}
