/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.extension.sftp.internal.connection.StaticConnectionSource;
import org.mule.runtime.api.connection.ConnectionException;

import org.junit.Before;
import org.junit.Test;

public class StaticConnectionSourceUnitTestCase {

  public FileSystem fileSystem = mock(FileSystem.class);

  private StaticConnectionSource staticConnectionSource;

  @Before
  public void doSetup() {
    staticConnectionSource = new StaticConnectionSource(fileSystem);
  }

  @Test
  public void checkSameConnectionIsReturned() throws ConnectionException {
    assertThat(staticConnectionSource.getConnection(), is(fileSystem));
  }

  @Test
  public void checkSameConnectionIsReturnedAfterCallingRelease() throws ConnectionException {
    FileSystem retrievedFileSystem = staticConnectionSource.getConnection();
    staticConnectionSource.releaseConnection();
    assertThat(retrievedFileSystem, is(staticConnectionSource.getConnection()));
  }

}
