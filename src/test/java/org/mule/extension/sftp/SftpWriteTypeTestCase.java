/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_WORLD;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import io.qameta.allure.Feature;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@Feature(SFTP_EXTENSION)
public class SftpWriteTypeTestCase extends CommonSftpConnectorTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Sftp - String", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML, HELLO_WORLD, HELLO_WORLD},
        {"Sftp - native byte", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML, "A".getBytes()[0], "A"},
        {"Sftp - Object byte", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML, new Byte("A".getBytes()[0]),
            "A"},
        {"Sftp - byte[]", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML, HELLO_WORLD.getBytes(),
            HELLO_WORLD},
        {"Sftp - OutputHandler", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML, new TestOutputHandler(),
            HELLO_WORLD},
        {"Sftp - InputStream", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML,
            new ByteArrayInputStream(HELLO_WORLD.getBytes()),
            HELLO_WORLD},});
  }

  private final Object content;
  private final String expected;
  private String path;

  public SftpWriteTypeTestCase(String name, SftpTestHarness testHarness, String ftpConfigFile, Object content, String expected) {
    super(name, testHarness, ftpConfigFile);
    this.content = content;
    this.expected = expected;
  }

  @Override
  protected String getConfigFile() {
    return "sftp-write-config.xml";
  }


  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    final String folder = "test";
    testHarness.makeDir(folder);
    path = folder + "/test.txt";
  }

  @Test
  public void writeAndAssert() throws Exception {
    write(content);
    assertThat(readPathAsString(path), equalTo(expected));
  }

  private void write(Object content) throws Exception {
    doWrite(path, content, FileWriteMode.APPEND, false);
  }

  private static class TestOutputHandler implements OutputHandler {

    @Override
    public void write(CoreEvent event, OutputStream out) throws IOException {
      org.apache.commons.io.IOUtils.write(HELLO_WORLD, out);
    }
  }

}
