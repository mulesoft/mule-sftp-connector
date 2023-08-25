/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;

import io.qameta.allure.Feature;

@Feature(SFTP_EXTENSION)
public class SftpMoveTestCase extends SftpCopyTestCase {

  public SftpMoveTestCase(String name, SftpTestHarness testHarness, String ftpConfigFile) {
    super(name, testHarness, ftpConfigFile);
  }

  @Override
  protected String getConfigFile() {
    return "sftp-move-config.xml";
  }

  @Override
  protected String getFlowName() {
    return "move";
  }

  @Override
  protected void assertCopy(String target) throws Exception {
    super.assertCopy(target);
    assertThat(testHarness.fileExists(sourcePath), is(false));
  }
}
