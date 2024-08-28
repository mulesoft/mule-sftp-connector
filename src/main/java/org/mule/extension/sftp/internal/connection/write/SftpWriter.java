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

public interface SftpWriter {

  void write(String path, InputStream stream, FileWriteMode mode, URI uri) throws IOException;

}