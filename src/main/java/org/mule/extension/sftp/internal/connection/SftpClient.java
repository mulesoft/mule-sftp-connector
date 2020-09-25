/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_PERMISSION_DENIED;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import static org.mule.extension.sftp.internal.SftpUtils.resolvePathOrResource;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;
import static java.lang.String.format;

import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.sftp.api.SftpConnectionException;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.random.alg.PRNGAlgorithm;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around jsch sftp library which provides access to basic sftp commands.
 *
 * @since 1.0
 */
public class SftpClient {

  private static Logger LOGGER = LoggerFactory.getLogger(SftpClient.class);

  private static SftpLogger JSCH_LOGGER = new SftpLogger();

  public static final String CHANNEL_SFTP = "sftp";
  public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
  public static final String PREFERRED_AUTHENTICATION_METHODS = "PreferredAuthentications";

  static {
    JSch.setLogger(JSCH_LOGGER);
  }

  private ChannelSftp sftp;
  private JSch jsch;
  private Session session;
  private final String host;
  private int port = 22;
  private String password;
  private String identityFile;
  private String passphrase;
  private String knownHostsFile;
  private String preferredAuthenticationMethods;
  private long connectionTimeoutMillis = 0; // No timeout by default
  private SftpProxyConfig proxyConfig;
  private String prngAlgorithmClassImplementation;

  private SftpFileSystem owner;

  /**
   * Creates a new instance which connects to a server on a given {@code host} and {@code port}
   *
   * @param host the host address
   * @param port the remote connection port
   * @param jSchSupplier a {@link Supplier} for obtaining a {@link JSch} client
   */
  public SftpClient(String host, int port, Supplier<JSch> jSchSupplier, PRNGAlgorithm prngAlgorithm) {
    this.host = host;
    this.port = port;
    this.prngAlgorithmClassImplementation = prngAlgorithm.getImplementationClassName();
    jsch = jSchSupplier.get();
  }

  /**
   * @return the current working directory
   */
  public String getWorkingDirectory() {
    try {
      return sftp.pwd();
    } catch (SftpException e) {
      throw exception("Could not obtain current working directory", e);
    }
  }

  /**
   * Changes the current working directory to {@code wd}
   *
   * @param path the new working directory path
   */
  public void changeWorkingDirectory(String path) {
    LOGGER.debug("Attempting to cwd to: {}", path);

    try {
      sftp.cd(normalizePath(path));
    } catch (SftpException e) {
      throw exception("Exception occurred while trying to change working directory to " + path, e);
    }
  }

  /**
   * Gets the attributes for the file in the given {code path}
   *
   * @param uri the file's uri
   * @return a {@link SftpFileAttributes} or {@code null} if the file doesn't exist.
   */
  public SftpFileAttributes getAttributes(URI uri) {
    try {
      return new SftpFileAttributes(uri, sftp.stat(normalizePath(uri.getPath())));
    } catch (SftpException e) {
      if (e.id == SSH_FX_NO_SUCH_FILE) {
        return null;
      }
      throw exception("Could not obtain attributes for path " + uri, e);
    }
  }

  /**
   * Performs a login operation for the given {@code user} using the connection options and additional credentials optionally set
   * on this client
   *
   * @param user the authentication user
   */
  public void login(String user) throws IOException, JSchException {
    configureSession(user);
    if (!isEmpty(password)) {
      session.setPassword(password);
    }

    if (!isEmpty(identityFile)) {
      setupIdentity();
    }

    connect();
  }

  private void setupIdentity() throws JSchException {
    if (passphrase == null || "".equals(passphrase)) {
      jsch.addIdentity(identityFile);
    } else {
      jsch.addIdentity(identityFile, passphrase);
    }
  }

  private void checkExists(String path) {
    if (!new File(normalizePath(path)).exists()) {
      throw new IllegalArgumentException(format("File '%s' not found", path));
    }
  }

  private void connect() throws JSchException {
    session.connect();
    Channel channel = session.openChannel(CHANNEL_SFTP);
    channel.connect();

    sftp = (ChannelSftp) channel;
  }

  private void configureSession(String user) throws JSchException {
    Properties hash = new Properties();
    configureHostChecking(hash);
    setRandomPrng(hash);
    if (!isEmpty(preferredAuthenticationMethods)) {
      hash.put(PREFERRED_AUTHENTICATION_METHODS, preferredAuthenticationMethods);
    }

    session = jsch.getSession(user, host);
    session.setConfig(hash);
    session.setPort(port);
    session.setTimeout(Long.valueOf(connectionTimeoutMillis).intValue());
    configureProxy(session);
  }

  private void setRandomPrng(Properties hash) {
    hash.put("random", prngAlgorithmClassImplementation);
  }

  private void configureHostChecking(Properties hash) throws JSchException {
    if (knownHostsFile == null) {
      hash.put(STRICT_HOST_KEY_CHECKING, "no");
    } else {
      checkExists(knownHostsFile);
      hash.put(STRICT_HOST_KEY_CHECKING, "ask");
      jsch.setKnownHosts(knownHostsFile);
    }
  }

  private void configureProxy(Session session) {
    if (proxyConfig != null) {

      Proxy proxy = null;
      switch (proxyConfig.getProtocol()) {
        case HTTP:
          ProxyHTTP proxyHttp = new ProxyHTTP(proxyConfig.getHost(), proxyConfig.getPort());
          if (proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
            proxyHttp.setUserPasswd(proxyConfig.getUsername(), proxyConfig.getPassword());
          }
          proxy = proxyHttp;
          break;

        case SOCKS4:
          ProxySOCKS4 proxySocks4 = new ProxySOCKS4(proxyConfig.getHost(), proxyConfig.getPort());
          if (proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
            proxySocks4.setUserPasswd(proxyConfig.getUsername(), proxyConfig.getPassword());
          }
          proxy = proxySocks4;
          break;

        case SOCKS5:
          ProxySOCKS5 proxySocks5 = new ProxySOCKS5(proxyConfig.getHost(), proxyConfig.getPort());
          if (proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
            proxySocks5.setUserPasswd(proxyConfig.getUsername(), proxyConfig.getPassword());
          }
          proxy = proxySocks5;
          break;

        default:
          // should never get here, except a new type was added to the enum and not handled
          throw new IllegalArgumentException(format("Proxy protocol %s not recognized", proxyConfig.getProtocol()));
      }

      session.setProxy(proxy);
    }
  }

  /**
   * Renames the file at {@code sourcePath} to {@code target}
   *
   * @param sourcePath the path to the renamed file
   * @param target the new path
   */
  public void rename(String sourcePath, String target) throws IOException {
    try {
      sftp.rename(normalizePath(sourcePath), normalizePath(target));
    } catch (SftpException e) {
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
      sftp.rm(normalizePath(path));
    } catch (SftpException e) {
      throw exception("Could not delete file " + path, e);
    }
  }

  /**
   * Closes the active session and severs the connection (if any of those were active)
   */
  public void disconnect() {
    if (sftp != null && sftp.isConnected()) {
      sftp.exit();
      sftp.disconnect();
    }

    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }

  /**
   * @return whether this client is currently connected and logged into the remote server
   */
  public boolean isConnected() {
    return sftp != null && sftp.isConnected() && !sftp.isClosed() && session != null && session.isConnected();
  }

  /**
   * Lists the contents of the directory at the given {@code path}
   *
   * @param path the path to list
   * @return a immutable {@link List} of {@Link SftpFileAttributes}. Might be empty but will never be {@code null}
   */
  public List<SftpFileAttributes> list(String path) {
    List<ChannelSftp.LsEntry> entries;
    try {
      entries = sftp.ls(normalizePath(path));
    } catch (SftpException e) {
      throw exception("Found exception trying to list path " + path, e);
    }

    if (isEmpty(entries)) {
      return emptyList();
    }

    return entries.stream().map(entry -> new SftpFileAttributes(createUri(path, entry.getFilename()), entry.getAttrs()))
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
      return sftp.get(normalizePath(path));
    } catch (SftpException e) {
      throw exception("Exception was found trying to retrieve the contents of file " + path, e);
    }
  }

  /**
   * Writes the contents of the {@code stream} into the file at the given {@code path}
   *
   * @param path the path to write into
   * @param stream the content to be written
   * @param mode the write mode
   * @throws Exception if anything goes wrong
   */
  public void write(String path, InputStream stream, FileWriteMode mode) throws Exception {
    sftp.put(stream, path, toInt(mode));
  }

  /**
   * Opens an {@link OutputStream} which allows writing into the file pointed by {@code path}
   *
   * @param path the path to write into
   * @param mode the write mode
   * @return an {@link OutputStream}
   */
  public OutputStream getOutputStream(String path, FileWriteMode mode) throws Exception {
    return sftp.put(normalizePath(path), toInt(mode));
  }

  private int toInt(FileWriteMode mode) {
    return mode == FileWriteMode.APPEND ? ChannelSftp.APPEND : ChannelSftp.OVERWRITE;
  }

  /**
   * Creates a directory
   *
   * @param directoryName The directory name
   * @throws IOException If an error occurs
   */
  public void mkdir(String directoryName) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Trying to create directory " + directoryName);
      }
      sftp.mkdir(normalizePath(directoryName));
    } catch (SftpException e) {
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
      sftp.rmdir(path);
    } catch (SftpException e) {
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
      if (cause.getCause() instanceof IOException) {
        return exception(message, new ConnectionException(cause, owner));
      }
      if (((SftpException) cause).id == SSH_FX_PERMISSION_DENIED) {
        return new ModuleException(message, FileError.ACCESS_DENIED, cause);
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
    this.connectionTimeoutMillis = connectionTimeoutMillis;
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

  public void setOwner(SftpFileSystem owner) {
    this.owner = owner;
  }
}
