/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.sampledata;

import org.mule.extension.file.common.api.BaseFileSystemOperations;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;

public class ObjectSampleDataProvider implements SampleDataProvider {

  @Parameter
  String directoryPath;

  @Config
  FileConnectorConfig config;

  @Connection
  FileSystem connection;


  @Override
  public String getId() {
    return getClass().getSimpleName();
  }

  @Override
  public Result<InputStream, FileAttributes> getSample() throws SampleDataException {

    connection.changeToBaseDir();
    List<Result<InputStream, FileAttributes>> files = connection.list(config, directoryPath, false, null, null);

    if (files.isEmpty()) {
      throw new SampleDataException("No data available", SampleDataException.NO_DATA_AVAILABLE);
    }

    return files.get(0);
  }
}
