/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.file.common.api.exceptions.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;

import java.nio.file.Paths;

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
    final String base = testHarness.getWorkingDirectory();
    doCreateDirectory(Paths.get(base).resolve(DIRECTORY).toAbsolutePath().toString());

    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  @Test
  public void createDirectoryFromRoot() throws Exception {
    String rootChildDirectoryPath =
        normalizePath(Paths.get(testHarness.getRootDirectory()).resolve(ROOT_CHILD_DIRECTORY).toAbsolutePath().toString());
    doCreateDirectory(rootChildDirectoryPath);
    assertThat(testHarness.dirExists(rootChildDirectoryPath), is(true));
  }

  private void doCreateDirectory(String directory) throws Exception {
    flowRunner("createDirectory").withVariable("directory", directory).run();
  }
}
