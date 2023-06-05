/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static org.mule.extension.sftp.common.api.FileTestHarness.WORKING_DIR;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.sftp.SftpTestHarness;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MediaType;

import java.io.IOException;
import java.net.URI;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class FileMessageMediaTypeTestCase {

  private static final String TEMP_DIRECTORY = "files";

  @ClassRule
  public static SftpTestHarness testHarness = new SftpTestHarness();

  private SftpClient client;

  private static final String fileContent = "File Content.";

  private SftpFileSystem fileSystem;
  private SftpFileAttributes sftpFileAttributesMock;

  @Before
  public void setUp() throws Exception {
    if (!testHarness.dirExists(TEMP_DIRECTORY)) {
      testHarness.makeDir(TEMP_DIRECTORY);
    }
    setUpMocks();
    fileSystem = new SftpFileSystem(client, "/", mock(LockFactory.class));
  }

  @Test
  public void testMediaTypeForTextFile() throws Exception {
    String expectedMediaType = "text/plain";

    String fileName = "file.txt";
    String fullPath = getFullPath(fileName);
    testHarness.write(fullPath, fileContent);
    testHarness.getFileList("/");

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test
  public void testMediaTypeForXMLFile() throws Exception {
    String expectedMediaType = "application/xml";

    String fileName = "file.xml";
    String fullPath = getFullPath(fileName);
    testHarness.write(fullPath, fileContent);
    testHarness.getFileList("/");

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test
  public void testMediaTypeForJSONFile() throws Exception {
    String expectedMediaType = "application/json";

    String fileName = "file.json";
    String fullPath = getFullPath(fileName);
    testHarness.write(fullPath, fileContent);
    testHarness.getFileList("/");

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test
  public void testMediaTypeForUnknownFile() throws Exception {
    String expectedMediaType = "application/octet-stream";
    String fileName = "file.abc";
    String fullPath = getFullPath(fileName);
    testHarness.write(fullPath, fileContent);

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test
  public void testWithEmptyFilePath() {
    String expectedMediaType = "application/octet-stream";
    String fileName = "";

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test
  public void testWithDirectoryPath() throws Exception {
    String expectedMediaType = "application/octet-stream";
    String fileName = "/directory/";
    String fullPath = getFullPath(fileName);
    testHarness.makeDir(fullPath);

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test(
  // expected = NullPointerException.class
  )
  public void testWithNullFilePath() {
    String expectedMediaType = "application/octet-stream";
    String fileName = null;

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  @Test
  public void testWithNoExtension() {
    String expectedMediaType = "application/octet-stream";
    String fileName = "file";

    when(sftpFileAttributesMock.getPath()).thenReturn(fileName);

    MediaType actualMediaType = fileSystem.getFileMessageMediaType(sftpFileAttributesMock);

    assertEquals(expectedMediaType, actualMediaType.toString());
  }

  private String getFullPath(String fileName) {
    return "/" + WORKING_DIR + "/" + TEMP_DIRECTORY + "/" + fileName;
  }

  private void setUpMocks() throws IOException {
    client = mock(SftpClient.class);
    sftpFileAttributesMock = mock(SftpFileAttributes.class);
    client = mock(SftpClient.class);
    when(client.getAttributes(any(URI.class))).thenReturn(sftpFileAttributesMock);
  }
}
