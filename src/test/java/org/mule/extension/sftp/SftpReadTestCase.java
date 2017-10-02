/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.test.extension.file.common.api.FileTestHarness.BINARY_FILE_NAME;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_PATH;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_WORLD;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;

import java.nio.file.Paths;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(SFTP_EXTENSION)
public class SftpReadTestCase extends CommonSftpConnectorTestCase {

  public SftpReadTestCase(String name, SftpTestHarness testHarness, String ftpConfigFile) {
    super(name, testHarness, ftpConfigFile);
  }

  @Override
  protected String getConfigFile() {
    return "sftp-read-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    testHarness.createHelloWorldFile();
  }

  @Test
  public void read() throws Exception {
    Message message = readHelloWorld().getMessage();

    assertThat(message.getPayload().getDataType().getMediaType().getPrimaryType(), is(JSON.getPrimaryType()));
    assertThat(message.getPayload().getDataType().getMediaType().getSubType(), is(JSON.getSubType()));

    assertThat(toString(message.getPayload().getValue()), is(HELLO_WORLD));
  }

  @Test
  public void readBinary() throws Exception {
    testHarness.createBinaryFile();

    Message response = readPath(BINARY_FILE_NAME, false);

    assertThat(response.getPayload().getDataType().getMediaType().getPrimaryType(), is(MediaType.BINARY.getPrimaryType()));
    assertThat(response.getPayload().getDataType().getMediaType().getSubType(), is(MediaType.BINARY.getSubType()));

    AbstractFileInputStream payload = (AbstractFileInputStream) response.getPayload().getValue();
    assertThat(payload.isLocked(), is(false));

    byte[] readContent = new byte[new Long(HELLO_WORLD.length()).intValue()];
    org.apache.commons.io.IOUtils.read(payload, readContent);
    assertThat(new String(readContent), is(HELLO_WORLD));
  }

  @Test
  public void readWithForcedMimeType() throws Exception {
    CoreEvent event = flowRunner("readWithForcedMimeType").withVariable("path", HELLO_PATH).run();
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(), equalTo("test"));
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getSubType(), equalTo("test"));
  }

  @Test
  public void readUnexisting() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exist");
    readPath("files/not-there.txt");
  }

  @Test
  public void readDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "since it's a directory");
    readPath("files");
  }

  @Test
  public void readLockReleasedOnContentConsumed() throws Exception {
    Message message = readWithLock();
    getPayloadAsString(message);

    assertThat(isLocked(message), is(false));
  }

  @Test
  public void readLockReleasedOnEarlyClose() throws Exception {
    Message message = readWithLock();
    assertThat(isLocked(message), is(false));
  }


  @Test
  public void getProperties() throws Exception {
    SftpFileAttributes fileAttributes = (SftpFileAttributes) readHelloWorld().getMessage().getAttributes().getValue();
    testHarness.assertAttributes(HELLO_PATH, fileAttributes);
  }

  private Message readWithLock() throws Exception {
    Message message =
        flowRunner("readWithLock").withVariable("readPath", Paths.get("files/hello.json").toString()).run().getMessage();
    return message;
  }
}
