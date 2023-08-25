/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mule.extension.file.common.api.exceptions.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;

import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(SFTP_EXTENSION)
public class SftpCreateDirectoryTestCase extends CommonSftpConnectorTestCase {

  private static final String DIRECTORY = "validDirectory";
  private static final String ROOT_CHILD_DIRECTORY = "rootChildDirectory";

  public SftpCreateDirectoryTestCase(String name, SftpTestHarness testHarness, String ftpConfigFile) {
    super(name, testHarness, ftpConfigFile);
  }

  @Override
  protected String getConfigFile() {
    return "sftp-create-directory-config.xml";
  }

  @Test
  public void createDirectory() throws Exception {
    doCreateDirectory(DIRECTORY);
    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  @Test
  public void createExistingDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    final String directory = "washerefirst";
    testHarness.makeDir(directory);
    doCreateDirectory(directory);
  }

  @Test
  public void createDirectoryWithComplexPath() throws Exception {
    String complexPath = createUri(testHarness.getWorkingDirectory(), DIRECTORY).getPath();
    doCreateDirectory(complexPath);

    assertThat(testHarness.dirExists(complexPath), is(true));
  }

  @Test
  public void createDirectoryFromRoot() throws Exception {
    String rootChildDirectoryPath = createUri(testHarness.getAbsoluteRootDirectory(), ROOT_CHILD_DIRECTORY).getPath();
    doCreateDirectory(rootChildDirectoryPath);
    assertThat(testHarness.dirExists(rootChildDirectoryPath), is(true));
  }

  @Test
  public void createRootDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("/");
  }

  @Test
  public void createRootCurrentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("/.");
  }

  @Test
  public void createRootParentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("/..");
  }

  @Test
  public void createCurrentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory(".");
  }

  @Test
  public void createParentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("..");
  }

  @Test
  public void createParentParentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("../..");
  }

  @Test
  public void createDirectoryTwice() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("zarasa/..");
  }

  @Test
  public void createCurrentDirectoryWithNonExistingParent() throws Exception {
    doCreateDirectory("zarasa/.");
    assertThat(testHarness.dirExists("zarasa"), is(true));
  }

  @Test
  public void createDirectoryEndingInSlash() throws Exception {
    doCreateDirectory("zarasa/");
    assertThat(testHarness.dirExists("zarasa"), is(true));
  }

  @Test
  public void createBlankDirectory() throws Exception {
    testHarness.expectedError().expectErrorType("SFTP", "ILLEGAL_PATH");
    testHarness.expectedError().expectMessage(containsString("directory path cannot be null nor blank"));
    doCreateDirectory("");
  }

  @Test
  public void createDirectoryWithSpace() throws Exception {
    testHarness.expectedError().expectErrorType("SFTP", "ILLEGAL_PATH");
    testHarness.expectedError().expectMessage(containsString("directory path cannot be null nor blank"));
    doCreateDirectory(" ");
  }

  @Test
  public void createComplexDirectoryWithSpace() throws Exception {
    doCreateDirectory("zarasa/ /valid");
    assertThat(testHarness.dirExists("zarasa/ "), is(true));
    assertThat(testHarness.dirExists("zarasa/ /valid"), is(true));
  }

  @Test
  public void createDirectoryWithSpaceAndSlash() throws Exception {
    doCreateDirectory(" /");
    assertThat(testHarness.dirExists(" "), is(true));
  }

  @Test
  public void createDirectoryWithSpecialCharacter() throws Exception {
    doCreateDirectory("@");
    assertThat(testHarness.dirExists("@"), is(true));
  }

  @Test
  public void createCurrentDirectoryAndChildDirectoryIgnoresDot() throws Exception {
    doCreateDirectory("./valid");
    assertThat(testHarness.dirExists("valid"), is(true));
  }

  @Test
  public void createParentDirectoryAndChildDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("../valid");
  }

  @Test
  public void createDirectoryStartingWithSlashCreatesAbsoluteDirectory() throws Exception {
    doCreateDirectory("/secondBase/child");
    assertThat(testHarness.dirExists("/secondBase/child"), is(true));
    assertThat(testHarness.dirExists("/base/secondBase/child"), is(false));
  }

  @Test
  public void createRelativeDirectoryResolvesCorrectly() throws Exception {
    testHarness.makeDir("child");
    doCreateDirectory("child/secondChild");
    assertThat(testHarness.dirExists("/base/child/secondChild"), is(true));
    assertThat(testHarness.dirExists("/base/child/child/secondChild"), is(false));
    assertThat(testHarness.dirExists("/base/child/child"), is(false));
  }

  @Test
  public void createDirectoryWithColon() throws Exception {
    //TODO: This assumption must stay as long as the test server runs in the same OS as the tests. It could be
    // removed when the test server always runs in an external Linux container.
    assumeTrue(!IS_OS_WINDOWS);
    final String path = "pathWith:Colon";
    doCreateDirectory(path);
    assertThat(testHarness.dirExists("/base/pathWith:Colon"), is(true));
  }

  @Test
  public void createDirectoryWithGreaterThan() throws Exception {
    //TODO: This assumption must stay as long as the test server runs in the same OS as the tests. It could be
    // removed when the test server always runs in an external Linux container.
    assumeTrue(!IS_OS_WINDOWS);
    final String path = "pathWith>";
    doCreateDirectory(path);
    assertThat(testHarness.dirExists("/base/pathWith>"), is(true));
  }

  private void doCreateDirectory(String directory) throws Exception {
    flowRunner("createDirectory").withVariable("directory", directory).run();
  }
}
