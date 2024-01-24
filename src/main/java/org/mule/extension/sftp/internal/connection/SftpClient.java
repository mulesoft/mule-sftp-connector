/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static org.mule.extension.sftp.internal.error.FileError.CONNECTIVITY;
import static org.mule.extension.sftp.internal.util.SftpUtils.normalizePath;
import static org.mule.extension.sftp.internal.util.SftpUtils.resolvePathOrResource;
import static org.mule.extension.sftp.internal.util.UriUtils.createUri;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.sshd.sftp.common.SftpConstants.SSH_FX_CONNECTION_LOST;
import static org.apache.sshd.sftp.common.SftpConstants.SSH_FX_NO_CONNECTION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.error.FileError;
import org.mule.extension.sftp.internal.exception.FileAccessDeniedException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.sftp.client.SftpClient.OpenMode;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.slf4j.Logger;

/**
 * Wrapper around apache sshd library which provides access to basic sftp commands.
 *
 * @since 1.0
 */
public class SftpClient {

  private static final Logger LOGGER = getLogger(SftpClient.class);
  protected static final OpenMode[] CREATE_MODES = {OpenMode.Write, OpenMode.Create};
  protected static final OpenMode[] APPEND_MODES = {OpenMode.Write, OpenMode.Append};
  private static final Long PWD_COMMAND_EXECUTION_TIMEOUT = 30L;
  private static final TimeUnit PWD_COMMAND_EXECUTION_TIMEOUT_UNIT = SECONDS;
  private static final String PWD_COMMAND = "pwd";

  private final SshClient client;
  private org.apache.sshd.sftp.client.SftpClient sftp;
  private ClientSession session;
  private final String host;
  private int port;
  private String password;
  private String identityFile;
  private String passphrase;
  private String knownHostsFile;

  private String preferredAuthenticationMethods;
  private long connectionTimeoutMillis = Long.MAX_VALUE;
  private SftpProxyConfig proxyConfig;

  private SftpFileSystemConnection owner;

  private String cwd = "/";
  private static final Object LOCK = new Object();
  private String home;

  protected SchedulerService schedulerService;

  /**
   * Creates a new instance which connects to a server on a given {@code host} and {@code port}
   *
   * @param host the host address
   * @param port the remote connection port
   */
  public SftpClient(String host, int port, PRNGAlgorithm prngAlgorithm, SchedulerService schedulerService,
                    SftpProxyConfig sftpProxyConfig) {
    this.host = host;
    this.port = port;
    this.schedulerService = schedulerService;
    this.proxyConfig = sftpProxyConfig;



    client = ClientBuilder.builder()
        .factory(MuleSftpClient::new)
        .randomFactory(prngAlgorithm.getRandomFactory())
        .build();

    client.start();

    //    if (proxyConfig != null) {
    //      ((JGitSshClient) client)
    //          .setProxyDatabase(remote -> new ProxyData(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyConfig.getHost(),
    //                                                                                                     proxyConfig.getPort())),
    //                                                    proxyConfig.getUsername(), proxyConfig.getPassword().toCharArray()));
    //    }

    if (nonNull(proxyConfig)) {
      ((MuleSftpClient) client).setProxyConfig(proxyConfig);
    }
  }


  /**
   * @return the current working directory
   */
  public String getWorkingDirectory() {
    return cwd;
  }

  /**
   * Changes the current working directory to {@code wd}
   *
   * @param path the new working directory path
   */
  public void changeWorkingDirectory(String path) {
    String normalizedPath = normalizeRemotePath(path);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Attempting to cwd to: {}", normalizedPath);
    }
    this.cwd = normalizedPath;
  }

  /**
   * Gets the attributes for the file in the given {code path}
   *
   * @param uri the file's uri
   * @return a {@link SftpFileAttributes} or {@code null} if the file doesn't exist.
   */
  public SftpFileAttributes getAttributes(URI uri) throws IOException {
    if (uri == null) {
      return null;
    }
    String path = normalizeRemotePath(uri.getPath());
    try {
      return new SftpFileAttributes(uri, sftp.stat(path));
    } catch (SftpException e) {
      if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
        return null;
      }
      throw handleException("Could not obtain attributes for path " + path, e);
    } catch (IOException e) {
      throw handleException("Could not obtain attributes for path " + path, e);
    }
  }

  /**
   * Performs a login operation for the given {@code user} using the connection options and additional credentials optionally set
   * on this client
   *
   * @param user the authentication user
   */
  public void login(String user) throws IOException, GeneralSecurityException {
    configureSession(user);


    session.auth().verify(connectionTimeoutMillis);

    connect();
  }

  private void setupIdentity() throws GeneralSecurityException, IOException {
    FilePasswordProvider passwordProvider;
    if (passphrase == null || "".equals(passphrase)) {
      passwordProvider = FilePasswordProvider.EMPTY;
    } else {
      passwordProvider = FilePasswordProvider.of(passphrase);
    }
    KeyPairResourceLoader loader = SecurityUtils.getKeyPairResourceParser();
    for (KeyPair kp : loader.loadKeyPairs(session, Paths.get(identityFile), passwordProvider)) {
      session.addPublicKeyIdentity(kp);
    }
  }

  private void checkExists(String path) {
    if (!new File(normalizePath(path)).exists()) {
      throw new IllegalArgumentException(format("File '%s' not found", path));
    }
  }

  private void connect() throws IOException {
    SftpConcurrentClientFactory scf = SftpConcurrentClientFactory.instance();
    sftp = scf.createSftpClient(session);
  }

  private void configureSession(String user) throws IOException, GeneralSecurityException {
    configureHostChecking();
    if (this.preferredAuthenticationMethods != null && !this.preferredAuthenticationMethods.isEmpty()) {
      CoreModuleProperties.PREFERRED_AUTHS.set(client, this.preferredAuthenticationMethods.toLowerCase());
    }

    session = client.connect(user, host, port)
        .verify(connectionTimeoutMillis)
        .getSession();

    if (!isEmpty(password)) {
      session.addPasswordIdentity(password);
    }

    if (!isEmpty(identityFile)) {
      setupIdentity();
    }
  }

  private void configureHostChecking() {
    if (nonNull(knownHostsFile)) {
      client
          .setServerKeyVerifier(new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, Paths.get(knownHostsFile)) {

            @Override
            protected boolean acceptKnownHostEntries(
                                                     ClientSession clientSession, SocketAddress remoteAddress,
                                                     PublicKey serverKey,
                                                     Collection<HostEntryPair> knownHosts) {
              if (GenericUtils.isEmpty(knownHosts)) {
                LOGGER.error("known_hosts collection is empty!");
              }
              return super.acceptKnownHostEntries(clientSession, remoteAddress, serverKey, knownHosts);
            }
          });
    }
  }

  /**
   * Renames the file at {@code sourcePath} to {@code target}
   *
   * @param sourcePath the path to the renamed file
   * @param target     the new path
   */
  public void rename(String sourcePath, String target) {
    try {
      sftp.rename(normalizeRemotePath(sourcePath), normalizeRemotePath(target));
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Renamed {} to {}", sourcePath, target);
      }
    } catch (IOException e) {
      throw handleException(format("Could not rename path '%s' to '%s'", sourcePath, target), e);
    }
  }

  /**
   * Deletes the file at the given {@code path}
   *
   * @param path the path to the file to be deleted
   */
  public void deleteFile(String path) {

    try {
      sftp.remove(normalizeRemotePath(path));
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Deleted file {}", path);
      }
    } catch (IOException e) {
      throw handleException("Could not delete file " + path, e);
    }
  }

  /**
   * Closes the active session and severs the connection (if any of those were active)
   */
  public void disconnect() {
    if (session != null) {
      try {
        session.close();
        sftp.close();
      } catch (IOException e) {
        LOGGER.warn("Error while closing: {}", e, e);
      }
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Disconnected from {}:{}", host, port);
    }
  }

  /**
   * @return whether this client is currently connected and logged into the remote server
   */
  public boolean isConnected() {
    return sftp != null && sftp.isOpen() && session != null && session.isOpen();
  }

  /**
   * Lists the contents of the directory at the given {@code path}
   *
   * @param path the path to list
   * @return a immutable {@link List} of {@link SftpFileAttributes}. Might be empty but will never be {@code null}
   */
  public List<SftpFileAttributes> list(String path) {
    Collection<org.apache.sshd.sftp.client.SftpClient.DirEntry> entries;
    try {
      entries = sftp.readEntries(normalizeRemotePath(path));
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Listed {} entries from path {}", entries.size(), path);
      }
    } catch (IOException e) {
      throw handleException("Found exception trying to list path " + path, e);
    }

    if (isEmpty(entries)) {
      return emptyList();
    }

    return entries.stream().map(entry -> new SftpFileAttributes(createUri(path, entry.getFilename()), entry.getAttributes()))
        .collect(toImmutableList());
  }

  /**
   * An {@link InputStream} with the contents of the file at the given {@code path}
   *
   * @param path the path to the file to read
   * @return an {@link InputStream}
   */
  public InputStream getFileContent(String path) {
    try {
      return sftp.read(normalizeRemotePath(path));
    } catch (IOException e) {
      throw handleException("Exception was found trying to retrieve the contents of file " + path, e);
    }
  }

  /**
   * Writes the contents of the {@code stream} into the file at the given {@code path}
   *
   * @param path   the path to write into
   * @param stream the content to be written
   * @param mode   the write mode
   */
  public void write(String path, InputStream stream, FileWriteMode mode) throws IOException {
    try (OutputStream out = getOutputStream(path, mode)) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = stream.read(buf)) != -1) {
        out.write(buf, 0, n);
      }
    }
  }

  /**
   * Returns the home directory of the sftp server
   *
   * @return a {@link String}
   */
  public String getHome() {
    try {
      synchronized (LOCK) {
        if (home == null) {
          home = executePWDCommandWithTimeout();
        }
      }
      return home;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Apache Mina 2.9.2 - have a hardcoded timeout to infinite on executeRemoteCommand, if this method fails, the operation hangs.
   * The solution at the moment is to add a timeout with the tools provided by mule sdk.
   */
  private String executePWDCommandWithTimeout() throws IOException {
    try {
      Scheduler getHomeScheduler =
          schedulerService.cpuLightScheduler(SchedulerConfig.config().withShutdownTimeout(PWD_COMMAND_EXECUTION_TIMEOUT,
                                                                                          PWD_COMMAND_EXECUTION_TIMEOUT_UNIT));
      Future<String> submit = getHomeScheduler.submit(() -> session.executeRemoteCommand(PWD_COMMAND));
      return submit.get().trim();
    } catch (InterruptedException e) {
      throw new MuleRuntimeException(e);
    } catch (Exception ex) {
      throw new IllegalPathException("Unable to resolve the working directory from server timed out. Please configure a valid working directory or use absolute paths on your operation.",
                                     ex);
    }
  }

  /**
   * Opens an {@link OutputStream} which allows writing into the file pointed by {@code path}
   *
   * @param path the path to write into
   * @param mode the write mode
   * @return an {@link OutputStream}
   */
  public OutputStream getOutputStream(String path, FileWriteMode mode) throws IOException {
    return sftp.write(normalizeRemotePath(path), toApacheSshdModes(mode));
  }

  private OpenMode[] toApacheSshdModes(FileWriteMode mode) {
    OpenMode[] modes;
    switch (mode) {
      case CREATE_NEW:
      case OVERWRITE:
        modes = CREATE_MODES;
        break;
      case APPEND:
        modes = APPEND_MODES;
        break;
      default:
        throw new IllegalArgumentException();
    }
    return modes;
  }

  /**
   * Creates a directory
   *
   * @param directoryName The directory name
   */
  public void mkdir(String directoryName) {
    try {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Trying to create directory {}", directoryName);
      }
      sftp.mkdir(normalizeRemotePath(directoryName));
    } catch (IOException e) {
      throw handleException("Could not create the directory " + directoryName, e);
    }
  }

  /**
   * Deletes the directory at {@code path}.
   * <p>
   * The directory is expected to be empty
   *
   * @param path the path of the directory to be deleted
   */
  public void deleteDirectory(String path) {
    try {
      sftp.rmdir(normalizeRemotePath(path));
    } catch (IOException e) {
      throw handleException("Could not delete directory " + path, e);
    }
  }

  public String getHost() {
    return host;
  }

  public void setPreferredAuthenticationMethods(String preferredAuthenticationMethods) {
    this.preferredAuthenticationMethods = preferredAuthenticationMethods;
  }

  public RuntimeException handleException(String message, Exception cause) {
    try {
      if (cause instanceof SftpException) {
        return handleSftpException(message, (SftpException) cause);
      } else if (cause instanceof IOException) {
        return handleIOException(message, (IOException) cause);
      }
    } catch (Exception ex) {
      return new MuleRuntimeException(createStaticMessage(message), cause);
    }
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  private RuntimeException handleSftpException(String message, SftpException cause) {
    int status = cause.getStatus();
    if (status == SSH_FX_CONNECTION_LOST || status == SSH_FX_NO_CONNECTION) {
      return handleException(message, new SftpConnectionException("Error occurred while trying to connect to host",
                                                                  new ConnectionException(cause, owner), CONNECTIVITY, owner));
    } else if (status == SftpConstants.SSH_FX_PERMISSION_DENIED) {
      return new FileAccessDeniedException(message, cause);
    }
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  private RuntimeException handleIOException(String message, IOException cause) {
    if (!sftp.isOpen()) {
      return handleException(message, new SftpConnectionException("Error occurred while trying to connect to host",
                                                                  new ConnectionException(cause, owner), CONNECTIVITY, owner));
    }
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  public void setKnownHostsFile(String knownHostsFile) {
    this.knownHostsFile =
        !isEmpty(knownHostsFile) ? new File(resolvePathOrResource(knownHostsFile)).getAbsolutePath() : knownHostsFile;

  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setIdentity(String identityFilePath, String passphrase) {
    if (!isEmpty(identityFilePath)) {
      String resolvedPath = resolvePathOrResource(identityFilePath);
      this.identityFile = new File(resolvedPath).getAbsolutePath();
      checkExists(resolvedPath);
    }
    this.passphrase = passphrase;
  }

  public int getPort() {
    return port;
  }

  public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
    this.connectionTimeoutMillis = connectionTimeoutMillis == 0 ? Long.MAX_VALUE : connectionTimeoutMillis;
  }

  public void setProxyConfig(SftpProxyConfig proxyConfig) throws ConnectionException {
    if (proxyConfig != null) {
      if (proxyConfig.getHost() == null || proxyConfig.getPort() == null) {
        throw new SftpConnectionException("SFTP Proxy must have both \"host\" and \"port\" set", FileError.CONNECTIVITY);
      }

      if ((proxyConfig.getUsername() == null) != (proxyConfig.getPassword() == null)) {
        throw new SftpConnectionException("SFTP Proxy requires both \"username\" and \"password\" if configured with authentication (otherwise none)",
                                          FileError.INVALID_CREDENTIALS);
      }

      this.proxyConfig = proxyConfig;
    }
  }

  public void setOwner(SftpFileSystemConnection owner) {
    this.owner = owner;
  }

  private String normalizeRemotePath(String path) {
    if (path.length() > 0 && path.charAt(0) == '/') {
      return normalizePath(path);
    } else {
      return normalizePath((cwd.equals("/") ? "" : cwd) + "/" + path);
    }
  }
}
