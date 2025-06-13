/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.api.metadata.MediaType;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.mule.extension.sftp.SftpTestHarness;
import org.mule.runtime.api.lock.LockFactory;

import java.net.URI;
import static org.mule.extension.sftp.common.api.FileTestHarness.WORKING_DIR;

@RunWith(Parameterized.class)
public class FileMessageMediaTypeForFilesWithoutContentTestCase {

  private SftpFileSystemConnection fileSystem;
  private SftpFileAttributes sftpFileAttributesMock;
  private static final String TEMP_DIRECTORY = "files";

  @ClassRule
  public static SftpTestHarness testHarness = new SftpTestHarness();

  private String fileName;
  private boolean isDirectory;

  public FileMessageMediaTypeForFilesWithoutContentTestCase(String fileName, boolean isDirectory) {
    this.fileName = fileName;
    this.isDirectory = isDirectory;
  }

  @Before
  public void setUp() throws Exception {
    if (!testHarness.dirExists(TEMP_DIRECTORY)) {
      testHarness.makeDir(TEMP_DIRECTORY);
    }
    SftpClient client = mock(SftpClient.class);
    sftpFileAttributesMock = mock(SftpFileAttributes.class);
    client = mock(SftpClient.class);
    when(client.getAttributes(any(URI.class))).thenReturn(sftpFileAttributesMock);
    fileSystem = new SftpFileSystemConnection(client, "/", mock(LockFactory.class));
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"file", false}, {"", false}, {null, false}, {"/directory/", true}});
  }

  @Test
  public void testMediaTypeForFilesWithoutContent() throws Exception {
    String expectedMediaType = "application/octet-stream";
    String fullPath = getFullPath(fileName);

    if (isDirectory) {
      testHarness.makeDir(fullPath);
    }

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  private String getFullPath(String fileName) {
    return "/" + WORKING_DIR + "/" + TEMP_DIRECTORY + "/" + fileName;
  }
}
