/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(SFTP_EXTENSION)
public class SftpDirectoryListenerFunctionalTestCase extends CommonSftpConnectorTestCase {

  private static final String MATCHERLESS_LISTENER_FOLDER_NAME = "matcherless";
  private static final String SHARED_LISTENER_FOLDER_NAME = "shared";
  private static final String WITH_MATCHER_FOLDER_NAME = "withMatcher";
  private static final String WATCH_FILE = "watchme.txt";
  private static final String WATCH_CONTENT = "who watches the watchmen?";
  private static final String DR_MANHATTAN = "Dr. Manhattan";
  private static final String MATCH_FILE = "matchme.txt";
  private static final int PROBER_TIMEOUT = 10000;
  private static final int PROBER_DELAY = 1000;
  private static final int NUMBER_OF_FILES = 10;

  private static List<Message> RECEIVED_MESSAGES;

  public static class TestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      RECEIVED_MESSAGES.add(event.getMessage());
      return event;
    }
  }

  public SftpDirectoryListenerFunctionalTestCase(String name, SftpTestHarness testHarness, String ftpConfigFile) {
    super(name, testHarness, ftpConfigFile);
  }

  @Override
  protected String getConfigFile() {
    return "sftp-directory-listener-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    testHarness.makeDir(MATCHERLESS_LISTENER_FOLDER_NAME);
    testHarness.makeDir(WITH_MATCHER_FOLDER_NAME);
    testHarness.makeDir(SHARED_LISTENER_FOLDER_NAME);

    RECEIVED_MESSAGES = new CopyOnWriteArrayList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    RECEIVED_MESSAGES = null;
  }

  private URI buildPath(String... path) throws Exception {
    return new URI(String.join("/", path));
  }

  @Test
  @Description("Verifies that a created file is picked")
  public void onFileCreated() throws Exception {
    URI file = buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);
    assertPoll(file, WATCH_CONTENT);
  }

  @Test
  @Description("Verifies that 10 files created are picked with a limited connection pool size with a post action active")
  public void onFilesCreatedWithLimitedPoolAndAutoDelete() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");
    startFlow("modifiedWatermark");
    for (int i = 0; i < NUMBER_OF_FILES; i++) {
      URI file = buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, format("file_%d.txt", i));
      testHarness.write(file.getPath(), WATCH_CONTENT);
    }
    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      assertThat(RECEIVED_MESSAGES, hasSize(NUMBER_OF_FILES));
      return true;
    });
  }

  @Test
  @Description("Verifies that files created in subdirs are picked")
  public void recursive() throws Exception {
    URI subdir = buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, "subdir");
    testHarness.makeDir(subdir.getPath());
    URI file = buildPath(subdir.getPath(), WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);

    assertPoll(file, WATCH_CONTENT);
  }

  @Test
  @Description("Verifies that files created in subdirs are not picked")
  public void nonRecursive() throws Exception {
    stopFlow("listenWithoutMatcher");

    startFlow("listenNonRecursive");
    URI subdir = buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, "subdir");
    testHarness.makeDir(subdir.getPath());
    URI file = buildPath(subdir.getPath(), WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);

    expectNot(file);

    file = buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, "nonRecursive.txt");
    final String nonRecursiveContent = "you shall not recurse";
    testHarness.write(file.getPath(), nonRecursiveContent);
    assertPoll(file, nonRecursiveContent);
  }

  @Test
  @Description("Verifies that only files compliant with the matcher are picked")
  public void matcher() throws Exception {
    final URI file = buildPath(WITH_MATCHER_FOLDER_NAME, MATCH_FILE);
    final URI rejectedFile = buildPath(WITH_MATCHER_FOLDER_NAME, WATCH_FILE);
    testHarness.write(file.getPath(), DR_MANHATTAN);
    testHarness.write(rejectedFile.getPath(), WATCH_CONTENT);

    assertPoll(file, DR_MANHATTAN);
    checkNot(PROBER_TIMEOUT, PROBER_DELAY, () -> RECEIVED_MESSAGES.size() > 1);
  }

  @Test
  @Description("Verifies that files are moved after processing")
  public void moveTo() throws Exception {
    stopFlow("listenWithoutMatcher");
    startFlow("moveTo");

    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files are moved after processing even if autoDelete is configured")
  public void moveToAndAutoDelete() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("moveToAndAutoDelete");

    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files that cannot be moved because a file already exists in the other directory with that name are deleted")
  public void moveToAndAutoDeleteWithSameFileName() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("moveToAndAutoDelete");
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
    RECEIVED_MESSAGES.clear();
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files that cannot be moved because a file already exists in the other directory with that name remain untouched")
  public void moveToWithSameFileName() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("moveTo");
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(buildPath(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
    RECEIVED_MESSAGES.clear();
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> testHarness.fileExists(buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
    checkNot(PROBER_TIMEOUT, PROBER_DELAY,
             () -> !testHarness.fileExists(buildPath(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files are moved and renamed after processing")
  public void moveToWithRename() throws Exception {
    stopFlow("listenWithoutMatcher");
    startFlow("moveToWithRename");

    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, "renamed.txt").getPath()));
  }

  @Test
  @Description("Tests the case of watermark on update timestamp, processing only files that have been modified after the prior poll")
  public void watermarkForModifiedFiles() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");


    final URI file = new URI(MATCHERLESS_LISTENER_FOLDER_NAME + "/" + WATCH_FILE);
    final URI file2 = new URI(MATCHERLESS_LISTENER_FOLDER_NAME + "/" + WATCH_FILE + "2");
    testHarness.write(file.getPath(), WATCH_CONTENT);
    testHarness.write(file2.getPath(), WATCH_CONTENT);

    startFlow("modifiedWatermark");
    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      if (RECEIVED_MESSAGES.size() == 2) {
        return RECEIVED_MESSAGES.stream().anyMatch(m -> containsPath(m, file.getPath())) &&
            RECEIVED_MESSAGES.stream().anyMatch(m -> containsPath(m, file2.getPath()));
      }

      return false;
    });

    assertThat(testHarness.fileExists(file.getPath()), is(true));
    assertThat(testHarness.fileExists(file2.getPath()), is(true));

    final String modifiedData = "modified!";
    RECEIVED_MESSAGES.clear();
    testHarness.write(file.getPath(), modifiedData);

    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      if (RECEIVED_MESSAGES.size() == 1) {
        Message message = RECEIVED_MESSAGES.get(0);
        return containsPath(message, file.getPath()) && message.getPayload().getValue().toString().contains(modifiedData);
      }

      return false;
    });
  }

  private boolean containsPath(Message message, String path) {
    SftpFileAttributes attrs = (SftpFileAttributes) message.getAttributes().getValue();
    return attrs.getPath().contains(path);
  }

  private void assertPoll(URI file, Object expectedContent) {
    Message message = expect(file);
    String payload = toString(message.getPayload().getValue());
    assertThat(payload, equalTo(expectedContent));
  }

  private Message expect(URI file) {
    Reference<Message> messageHolder = new Reference<>();
    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      getPicked(file).ifPresent(messageHolder::set);
      return messageHolder.get() != null;
    });

    return messageHolder.get();
  }

  private void expectNot(URI file) {
    checkNot(PROBER_TIMEOUT, PROBER_DELAY, () -> getPicked(file).isPresent());
  }

  private Optional<Message> getPicked(URI file) {
    return RECEIVED_MESSAGES.stream()
        .filter(message -> {
          FileAttributes attributes = (FileAttributes) message.getAttributes().getValue();
          return attributes.getPath().contains(file.getPath());
        })
        .findFirst();
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) getFlowConstruct(flowName)).stop();
  }
}
