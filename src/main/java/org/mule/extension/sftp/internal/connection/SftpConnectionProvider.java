/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.mule.extension.file.common.api.FileSystemProvider;
import org.mule.extension.sftp.api.SftpAuthenticationMethod;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.SftpConnector;
import org.mule.extension.sftp.internal.TimeoutSettings;
import org.mule.extension.sftp.random.alg.PRNGAlgorithm;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link FileSystemProvider} which provides instances of {@link SftpFileSystem} from instances of {@link SftpConnector}
 *
 * @since 1.0
 */
@DisplayName("SFTP Connection")
public class SftpConnectionProvider extends FileSystemProvider<SftpFileSystem>
    implements PoolingConnectionProvider<SftpFileSystem> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpConnectionProvider.class);

  private static final String TIMEOUT_CONFIGURATION = "Timeout Configuration";
  private static final String SFTP_ERROR_MESSAGE_MASK =
      "Could not establish SFTP connection with host: '%s' at port: '%d' - %s";
  private static final String AUTH_FAIL_MESSAGE = "Auth fail";
  private static final String SSH_DISCONNECTION_MESSAGE = "SSH_MSG_DISCONNECT";
  private static final String TIMEOUT = "timeout";

  private static AtomicBoolean alreadyLoggedConnectionTimeoutWarning = new AtomicBoolean(false);
  private static AtomicBoolean alreadyLoggedResponseTimeoutWarning = new AtomicBoolean(false);

  @Inject
  private LockFactory lockFactory;

  /**
   * The directory to be considered as the root of every relative path used with this connector. If not provided, it will default
   * to the remote server default.
   */
  @Parameter
  @Optional
  @Summary("The directory to be considered as the root of every relative path used with this connector")
  @DisplayName("Working Directory")
  private String workingDir = null;

  @ParameterGroup(name = TIMEOUT_CONFIGURATION)
  private TimeoutSettings timeoutSettings = new TimeoutSettings();

  @ParameterGroup(name = CONNECTION)
  private SftpConnectionSettings connectionSettings = new SftpConnectionSettings();

  /**
   * Set of authentication methods used by the SFTP client. Valid values are: GSSAPI_WITH_MIC, PUBLIC_KEY, KEYBOARD_INTERACTIVE
   * and PASSWORD.
   */
  @Parameter
  @Optional
  private Set<SftpAuthenticationMethod> preferredAuthenticationMethods;

  /**
   * If provided, the client will validate the server's key against the one in the referenced file. If the server key doesn't
   * match the one in the file, the connection will be aborted.
   */
  @Parameter
  @Optional
  @Path(type = FILE)
  @ExcludeFromConnectivitySchema
  private String knownHostsFile;

  /**
   * If provided, a proxy will be used for the connection.
   */
  @Parameter
  @Optional
  @Alias("sftp-proxy-config")
  private SftpProxyConfig proxyConfig;

  private SftpClientFactory clientFactory = new SftpClientFactory();

  @Override
  public SftpFileSystem connect() throws ConnectionException {
    checkConnectionTimeoutPrecision();
    checkResponseTimeoutPrecision();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Connecting to host: '%s' at port: '%d'", connectionSettings.getHost(), connectionSettings.getPort()));
    }
    SftpClient client = clientFactory.createInstance(connectionSettings.getHost(), connectionSettings.getPort());
    client.setConnectionTimeoutMillis(getConnectionTimeoutUnit().toMillis(getConnectionTimeout()));
    client.setPassword(connectionSettings.getPassword());
    client.setIdentity(connectionSettings.getIdentityFile(), connectionSettings.getPassphrase());
    if (preferredAuthenticationMethods != null && !preferredAuthenticationMethods.isEmpty()) {
      client.setPreferredAuthenticationMethods(join(preferredAuthenticationMethods, ","));
    }
    client.setKnownHostsFile(knownHostsFile);
    client.setProxyConfig(proxyConfig);
    try {
      client.login(connectionSettings.getUsername());
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new ConnectionException(getErrorMessage(connectionSettings, e.getMessage()), e);
    }

    return new SftpFileSystem(client, getWorkingDir(), lockFactory);
  }

  @Override
  public void disconnect(SftpFileSystem ftpFileSystem) {
    ftpFileSystem.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(SftpFileSystem ftpFileSystem) {
    return ftpFileSystem.validateConnection();
  }

  void setPort(int port) {
    connectionSettings.setPort(port);
  }

  void setHost(String host) {
    connectionSettings.setHost(host);
  }

  void setUsername(String username) {
    connectionSettings.setUsername(username);
  }

  void setPrngAlgorithm(PRNGAlgorithm algorithm) {
    connectionSettings.setPrngAlgorithm(algorithm);
  }

  void setPassword(String password) {
    connectionSettings.setPassword(password);
  }

  void setPassphrase(String passphrase) {
    connectionSettings.setPassphrase(passphrase);
  }

  void setIdentityFile(String identityFile) {
    connectionSettings.setIdentityFile(identityFile);
  }

  void setPreferredAuthenticationMethods(Set<SftpAuthenticationMethod> preferredAuthenticationMethods) {
    this.preferredAuthenticationMethods = preferredAuthenticationMethods;
  }

  void setKnownHostsFile(String knownHostsFile) {
    this.knownHostsFile = knownHostsFile;
  }

  public void setProxyConfig(SftpProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
  }

  void setClientFactory(SftpClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWorkingDir() {
    return workingDir;
  }

  protected Integer getConnectionTimeout() {
    return timeoutSettings.getConnectionTimeout();
  }

  protected TimeUnit getConnectionTimeoutUnit() {
    return timeoutSettings.getConnectionTimeoutUnit();
  }

  protected Integer getResponseTimeout() {
    return timeoutSettings.getResponseTimeout();
  }

  protected TimeUnit getResponseTimeoutUnit() {
    return timeoutSettings.getResponseTimeoutUnit();
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    timeoutSettings.setConnectionTimeout(connectionTimeout);
  }

  public void setConnectionTimeoutUnit(TimeUnit connectionTimeoutUnit) {
    timeoutSettings.setConnectionTimeoutUnit(connectionTimeoutUnit);
  }

  public void setResponseTimeout(Integer responseTimeout) {
    timeoutSettings.setResponseTimeout(responseTimeout);
  }

  public void setResponseTimeoutUnit(TimeUnit responseTimeoutUnit) {
    timeoutSettings.setResponseTimeoutUnit(responseTimeoutUnit);
  }

  private String getErrorMessage(SftpConnectionSettings connectionSettings, String message) {
    return format(SFTP_ERROR_MESSAGE_MASK, connectionSettings.getHost(), connectionSettings.getPort(), message);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }

    SftpConnectionProvider that = (SftpConnectionProvider) o;
    return Objects.equals(workingDir, that.workingDir) &&
        Objects.equals(timeoutSettings, that.timeoutSettings) &&
        Objects.equals(connectionSettings, that.connectionSettings) &&
        Objects.equals(preferredAuthenticationMethods, that.preferredAuthenticationMethods) &&
        Objects.equals(knownHostsFile, that.knownHostsFile) &&
        Objects.equals(proxyConfig, that.proxyConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), workingDir, timeoutSettings, connectionSettings, preferredAuthenticationMethods,
                        knownHostsFile, proxyConfig);
  }

  private void checkConnectionTimeoutPrecision() {
    if (!supportedTimeoutPrecision(getConnectionTimeoutUnit(), getConnectionTimeout())
        && alreadyLoggedConnectionTimeoutWarning.compareAndSet(false, true)) {
      LOGGER.warn("Connection timeout configuration not supported. Minimum value allowed is 1 millisecond.");
    }
  }

  private void checkResponseTimeoutPrecision() {
    if (!supportedTimeoutPrecision(getResponseTimeoutUnit(), getResponseTimeout())
        && alreadyLoggedResponseTimeoutWarning.compareAndSet(false, true)) {
      LOGGER.warn("Response timeout configuration not supported. Minimum value allowed is 1 millisecond.");
    }
  }

  private boolean supportedTimeoutPrecision(TimeUnit timeUnit, Integer timeout) {
    return timeUnit != null && timeout != null && (timeUnit.toMillis(timeout) >= 1 || timeout == 0);
  }
}
