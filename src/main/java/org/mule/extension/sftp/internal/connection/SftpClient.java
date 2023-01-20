/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.extension.file.common.api.exceptions.FileError.CONNECTIVITY;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import static org.mule.extension.sftp.internal.SftpUtils.resolvePathOrResource;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static java.lang.String.format;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient.OpenMode;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.sftp.api.SftpConnectionException;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.proxy.http.HttpClientConnector;
import org.mule.extension.sftp.internal.proxy.socks5.Socks5ClientConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around jsch sftp library which provides access to basic sftp commands.
 *
 * @since 1.0
 */
public class SftpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpClient.class);

  final private SshClient client = SshClient.setUpDefaultClient();
  private org.apache.sshd.sftp.client.SftpClient sftp;
  private ClientSession session;
  private final String host;
  private int port = 22;
  private String password;
  private String identityFile;
  private String passphrase;
  private String knownHostsFile;
  private String preferredAuthenticationMethods;
  private long connectionTimeoutMillis = Long.MAX_VALUE; // No timeout by default
  private SftpProxyConfig proxyConfig;

  private SftpFileSystem owner;

  private String cwd = "/";

  /**
   * Creates a new instance which connects to a server on a given {@code host} and {@code port}
   *
   * @param host the host address
   * @param port the remote connection port
   */
  public SftpClient(String host, int port) {
    this.host = host;
    this.port = port;
    client.start();
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
    this.cwd = path;
  }

  /**
   * Gets the attributes for the file in the given {code path}
   *
   * @param uri the file's uri
   * @return a {@link SftpFileAttributes} or {@code null} if the file doesn't exist.
   */
  public SftpFileAttributes getAttributes(URI uri) throws IOException {
    String path = normalizeRemotePath(uri.getPath());
    try {
      return new SftpFileAttributes(uri, sftp.stat(path));
    } catch (SftpException e) {
      if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
        return null;
      }
      throw exception("Could not obtain attributes for path " + path, e);
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
    if (!isEmpty(password)) {
      session.addPasswordIdentity(password);
    }

    if (!isEmpty(identityFile)) {
      setupIdentity();
    }

    session.auth().verify(connectionTimeoutMillis); // FIXME shorter timeout!

    connect();
  }

  private void setupIdentity() throws GeneralSecurityException, IOException {
    FilePasswordProvider passwordProvider;
    if (passphrase == null || "".equals(passphrase)) {
      // jsch.addIdentity(identityFile);
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
    org.apache.sshd.sftp.client.SftpClientFactory scf = org.apache.sshd.sftp.client.SftpClientFactory.instance();
    sftp = scf.createSftpClient(session);
  }

  private void configureSession(String user) throws IOException {

    session = client.connect(user, host, port)
        .verify(connectionTimeoutMillis)
        .getSession();

    session.setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);

    /*
     * Properties hash = new Properties(); configureHostChecking(hash); setRandomPrng(hash); if
     * (!isEmpty(preferredAuthenticationMethods)) { hash.put(PREFERRED_AUTHENTICATION_METHODS, preferredAuthenticationMethods); }
     */
    try {
      configureProxy(session);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void configureHostChecking(Properties hash) {
    if (knownHostsFile != null) {
      checkExists(knownHostsFile);
      client
          .setServerKeyVerifier(
                                new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, Paths.get(knownHostsFile)));
    }
  }

  private void configureProxy(ClientSession session) throws Exception {
    if (proxyConfig != null) {

      // Proxy proxy = null;
      InetSocketAddress proxyAddress = new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort());
      InetSocketAddress remoteAddress = new InetSocketAddress(this.host, this.port);
      switch (proxyConfig.getProtocol()) {
        case HTTP:
          session.setClientProxyConnector(new HttpClientConnector(proxyAddress, remoteAddress,
                                                                  proxyConfig.getUsername(),
                                                                  proxyConfig.getPassword().toCharArray()));
        case SOCKS5:
          session.setClientProxyConnector(new Socks5ClientConnector(proxyAddress, remoteAddress,
                                                                    proxyConfig.getUsername(),
                                                                    proxyConfig.getPassword().toCharArray()));
        default:
          // should never get here, except a new type was added to the enum and not handled
          throw new IllegalArgumentException(format("Proxy protocol %s not recognized", proxyConfig.getProtocol()));
      }
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
      throw exception(format("Could not rename path '%s' to '%s'", sourcePath, target), e);
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
      throw exception("Could not delete file " + path, e);
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
      // LOGGER.trace("Disconnected from {}:{}", session., session.getPort());
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
      throw exception("Found exception trying to list path " + path, e);
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
      throw exception("Exception was found trying to retrieve the contents of file " + path, e);
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
        modes = new OpenMode[] {OpenMode.Write, OpenMode.Create};
        break;
      case APPEND:
        modes = new OpenMode[] {OpenMode.Write, OpenMode.Append};
        break;
      case OVERWRITE:
        modes = new OpenMode[] {OpenMode.Write};
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
      throw exception("Could not create the directory " + directoryName, e);
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
      throw exception("Could not delete directory " + path, e);
    }
  }

  public String getHost() {
    return host;
  }

  public void setPreferredAuthenticationMethods(String preferredAuthenticationMethods) {
    this.preferredAuthenticationMethods = preferredAuthenticationMethods;
  }

  protected RuntimeException exception(String message, Exception cause) {
    if (cause instanceof SftpException) {
      // FIXME: Apache SSHD SftpException is never constructed with a cause. The following code will never run.
      if (cause.getCause() instanceof IOException) {
        return exception(message, new SftpConnectionException("Error occurred while trying to connect to host",
                                                              new ConnectionException(cause, owner), CONNECTIVITY,
                                                              owner));
      }
    }
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  private RuntimeException loginException(String user, Exception e) {
    return exception(format("Error during login to %s@%s", user, host), e);
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
        throw new SftpConnectionException(
                                          "SFTP Proxy requires both \"username\" and \"password\" if configured with authentication (otherwise none)",
                                          FileError.INVALID_CREDENTIALS);
      }

      this.proxyConfig = proxyConfig;
    }
  }

  public void setOwner(SftpFileSystem owner) {
    this.owner = owner;
  }

  private String normalizeRemotePath(String path) {
    return normalizePath(path.length() > 0 && path.charAt(0) == '/' ? path : (cwd.equals("/") ? "" : cwd) + "/" + path);
  }
}
