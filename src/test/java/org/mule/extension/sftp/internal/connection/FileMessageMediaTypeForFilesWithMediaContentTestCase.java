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
import org.junit.Test;

import java.net.URI;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.extension.sftp.SftpTestHarness;
import org.junit.ClassRule;
import static org.mule.extension.sftp.common.api.FileTestHarness.WORKING_DIR;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(Parameterized.class)
public class FileMessageMediaTypeForFilesWithMediaContentTestCase {

  private SftpFileSystemConnection fileSystem;
  private SftpFileAttributes sftpFileAttributesMock;

  @ClassRule
  public static SftpTestHarness testHarness = new SftpTestHarness();
  private static final String TEMP_DIRECTORY = "files";

  private String fileName;
  private String contentFilePath;
  private String expectedMediaType;

  public FileMessageMediaTypeForFilesWithMediaContentTestCase(String contentFilePath, String fileName, String expectedMediaType) {
    this.contentFilePath = contentFilePath;
    this.fileName = fileName;
    this.expectedMediaType = expectedMediaType;
  }

  private String getFullPath(String fileName) {
    return "/" + WORKING_DIR + "/" + TEMP_DIRECTORY + "/" + fileName;
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
    return Arrays.asList(new Object[][] {{"src/test/resources/sample.jpg", "fileContent.jpg", "image/jpeg"},
        {"src/test/resources/sample.ppt", "fileContent.ppt", "application/vnd.ms-powerpoint"}});
  }

  @Test
  public void testMediaTypeForFilesWithMediaContent() throws Exception {
    Path filePath = Paths.get(contentFilePath);
    byte[] fileBytes = Files.readAllBytes(filePath);

    // Convert the byte array to a string
    String fileContent = new String(fileBytes);
    String fullPath = getFullPath(fileName);
    testHarness.write(fullPath, fileContent);

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }
}
