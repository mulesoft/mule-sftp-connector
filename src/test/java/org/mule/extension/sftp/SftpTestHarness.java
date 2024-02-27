/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.mule.extension.sftp.SftpServer.PASSWORD;
import static org.mule.extension.sftp.SftpServer.USERNAME;
import static org.mule.extension.sftp.api.FileWriteMode.CREATE_NEW;
import static org.mule.extension.sftp.api.FileWriteMode.OVERWRITE;
import static org.mule.extension.sftp.internal.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.util.UriUtils.trimLastFragment;
import static org.mule.extension.sftp.internal.util.SftpUtils.normalizePath;
import static org.mule.extension.sftp.internal.util.SftpUtils.resolvePathOrResource;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.extension.AbstractSftpTestHarness;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpClientFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.extension.sftp.api.FileTestHarness;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

/**
 * Implementation of {@link FileTestHarness} for classic SFTP connections
 *
 * @since 1.0
 */
public class SftpTestHarness extends AbstractSftpTestHarness {

  private static final String SFTP_PORT = "SFTP_PORT";

  private TemporaryFolder temporaryFolder = new TemporaryFolder();
  private DynamicPort sftpPort = new DynamicPort(SFTP_PORT);
  private SftpServer sftpServer;
  private SftpClient sftpClient;
  private AuthConfigurator<SftpClient> clientAuthConfigurator;
  private AuthConfigurator<SftpServer> serverAuthConfigurator;

  @Inject
  protected SchedulerService schedulerService;

  /**
   * Creates a new instance which activates the {@code sftp} spring profile
   */
  public SftpTestHarness() {
    this(AuthType.USER_PASSWORD);
  }

  public SftpTestHarness(AuthType authType) {
    super("sftp");
    switch (authType) {
      case USER_PASSWORD:
        clientAuthConfigurator = (sftpClient -> sftpClient.setPassword(PASSWORD));
        serverAuthConfigurator = (SftpServer::setPasswordAuthenticator);
        break;
      case PUBLIC_KEY:
        clientAuthConfigurator = (sftpClient -> sftpClient.setIdentity(resolvePathOrResource("sftp-test-key"), null));
        serverAuthConfigurator = (sftpServer -> sftpServer
            .setPublicKeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get("sftp-test-key.pub"))));
    }
  }

  /**
   * Starts a SFTP server and connects a client to it
   */
  @Override
  protected void doBefore() throws Exception {
    temporaryFolder.create();
    setUpServer();
    sftpClient = createDefaultSftpClient();
    sftpClient.mkdir(WORKING_DIR);
    sftpClient.changeWorkingDirectory(WORKING_DIR);
    System.setProperty(WORKING_DIR_SYSTEM_PROPERTY, sftpClient.getWorkingDirectory());
  }

  /**
   * Disconnects the client and shuts the server down
   */
  @Override
  protected void doAfter() throws Exception {
    try {
      if (sftpClient != null) {
        sftpClient.disconnect();
      }

      if (sftpServer != null) {
        sftpServer.stop();
      }
    } finally {
      temporaryFolder.delete();
      System.clearProperty(WORKING_DIR_SYSTEM_PROPERTY);
    }
  }

  private SftpClient createDefaultSftpClient() throws IOException, GeneralSecurityException {
    SftpClient sftpClient =
        new SftpClientFactory().createInstance("localhost", sftpPort.getNumber(), PRNGAlgorithm.SHA1PRNG, schedulerService, null);
    clientAuthConfigurator.configure(sftpClient);

    sftpClient.setPassword(PASSWORD);
    sftpClient.login(USERNAME);
    return sftpClient;
  }

  public void setUpServer() {
    sftpServer = new SftpServer(sftpPort.getNumber(), temporaryFolder.getRoot().toPath());
    serverAuthConfigurator.configure(sftpServer);
    sftpServer.start();
  }

  /**
   * @return {@link #sftpPort}
   */
  @Override
  protected TestRule[] getChildRules() {
    return new TestRule[] {sftpPort};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createHelloWorldFile() throws Exception {
    final String dir = "files";
    makeDir(dir);
    write(dir, HELLO_FILE_NAME, HELLO_WORLD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createBinaryFile() throws Exception {
    sftpClient.write(BINARY_FILE_NAME, new ByteArrayInputStream(HELLO_WORLD.getBytes()), OVERWRITE, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void makeDir(String directoryPath) throws Exception {
    sftpClient.mkdir(directoryPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWorkingDirectory() throws Exception {
    return normalizePath(sftpClient.getWorkingDirectory());
  }

  public String getRootDirectory() throws Exception {
    return "/";
  }

  public String getAbsoluteRootDirectory() throws Exception {
    return temporaryFolder.getRoot().getAbsolutePath();
  }

  public String getAbsoluteWorkingDirectory() throws Exception {
    return normalizePath(getAbsoluteRootDirectory() + getWorkingDirectory());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String path, String content) throws Exception {
    // Does the append also create a file before????
    sftpClient.write(path, new ByteArrayInputStream(content.getBytes()), CREATE_NEW, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean dirExists(String path) throws Exception {
    FileAttributes attributes = sftpClient.getAttributes(createUri(path));
    return attributes != null && attributes.isDirectory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean fileExists(String path) throws Exception {
    return sftpClient.getAttributes(createUri(path)) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean changeWorkingDirectory(String path) throws Exception {
    try {
      sftpClient.changeWorkingDirectory(path);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getFileList(String path) throws Exception {
    List<String> files = sftpClient.list(path).stream().map(FileAttributes::getName).collect(toList());
    return files.toArray(new String[files.size()]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getServerPort() throws Exception {
    return sftpPort.getNumber();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertAttributes(String path, Object attributes) throws Exception {
    SftpFileAttributes fileAttributes = (SftpFileAttributes) attributes;
    SftpFileAttributes file = sftpClient.getAttributes(createUri(path));

    assertThat(fileAttributes.getName(), equalTo(file.getName()));
    assertThat(fileAttributes.getPath(), is(normalizePath(getWorkingDirectory() + "/" + HELLO_PATH)));
    assertThat(fileAttributes.getSize(), is(file.getSize()));
    assertThat(fileAttributes.getTimestamp(), equalTo(file.getTimestamp()));
    assertThat(fileAttributes.isDirectory(), is(false));
    assertThat(fileAttributes.isSymbolicLink(), is(false));
    assertThat(fileAttributes.isRegularFile(), is(true));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertDeleted(String path) throws Exception {
    URI directoryUri = createUri(sftpClient.getWorkingDirectory(), path);

    String lastFragment2 = FilenameUtils.getName(directoryUri.getPath());
    if (".".equals(lastFragment2)) {
      directoryUri = trimLastFragment(directoryUri);
    }

    assertThat(dirExists(directoryUri.getPath()), is(false));
  }

  public enum AuthType {
    USER_PASSWORD, PUBLIC_KEY
  }

  @FunctionalInterface
  private interface AuthConfigurator<T> {

    void configure(T sftpClient);
  }

  protected void writeByteByByteAsync(String path, String content, long delayBetweenCharacters) throws Exception {
    OutputStream os = sftpClient.getOutputStream(path, CREATE_NEW);

    new Thread(() -> {

      try {
        byte[] bytes = content.getBytes();
        for (int i = 0; i < bytes.length; i++) {
          IOUtils.copy(new ByteArrayInputStream(new byte[] {bytes[i]}), os);
          Thread.sleep(delayBetweenCharacters);
        }
      } catch (Exception e) {
        fail("Error trying to write in file");
      }
    }).start();

  }

  public SftpServer getSftpServer() {
    return sftpServer;
  }

}
