/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection.provider;

import static org.apache.sshd.common.SshConstants.SSH2_DISCONNECT_KEY_EXCHANGE_FAILED;
import static org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider.EDDSA_GAV;
import static org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider.EDDSA_PROVIDER_CLASS;
import static org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider.PROVIDER_FILE_NAME_PATTERN;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import static java.lang.String.format;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.sshd.common.SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.SftpAuthenticationMethod;
import org.mule.extension.sftp.internal.connection.FileBasedConfigProvider;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpClientFactory;
import org.mule.extension.sftp.internal.connection.SftpConnectionSettings;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.extension.sftp.internal.error.FileError;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.extension.sftp.internal.connection.TimeoutSettings;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.sshd.common.SshException;
import org.slf4j.Logger;

/**
 * An {@link FileSystemProvider} which provides instances of {@link SftpFileSystemConnection} from instances of {@link SftpConnector}
 *
 * @since 1.0
 */
@DisplayName("SFTP Connection")
@ExternalLib(name = "EDDSA Provider", description = "An EDDSA provider which provides support for ed25519 curve",
    nameRegexpMatcher = PROVIDER_FILE_NAME_PATTERN, requiredClassName = EDDSA_PROVIDER_CLASS, type = JAR,
    coordinates = EDDSA_GAV, optional = true)
public class SftpConnectionProvider extends FileSystemProvider<SftpFileSystemConnection>
    implements PoolingConnectionProvider<SftpFileSystemConnection> {

  private static final Logger LOGGER = getLogger(SftpConnectionProvider.class);

  private static final String TIMEOUT_CONFIGURATION = "Timeout Configuration";
  private static final String SECURITY_CONFIGURATION = "Security Configuration";
  private static final String SFTP_ERROR_MESSAGE_MASK =
      "Could not establish SFTP connection with host: '%s' at port: '%d' - %s";
  static final String PROVIDER_FILE_NAME_PATTERN = "(.*)\\.jar";
  static final String EDDSA_GAV = "net.i2p.crypto:eddsa:0.3.0";
  static final String EDDSA_PROVIDER_CLASS = "net.i2p.crypto.eddsa.EdDSASecurityProvider";

  private static AtomicBoolean alreadyLoggedConnectionTimeoutWarning = new AtomicBoolean(false);
  private static AtomicBoolean alreadyLoggedResponseTimeoutWarning = new AtomicBoolean(false);

  @Inject
  private LockFactory lockFactory;

  @Inject
  protected SchedulerService schedulerService;

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

  @ParameterGroup(name = SECURITY_CONFIGURATION)
  private final SecuritySettings securitySettings = new SecuritySettings();

  @ParameterGroup(name = CONNECTION)
  private SftpConnectionSettings connectionSettings = new SftpConnectionSettings();

  /**
   * Set of authentication methods used by the SFTP client. Valid values are: GSSAPI_WITH_MIC, PUBLIC_KEY
   * and PASSWORD.
   */
  @Parameter
  @Optional
  @NullSafe
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
  public SftpFileSystemConnection connect() throws ConnectionException {
    checkConnectionTimeoutPrecision();
    checkResponseTimeoutPrecision();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Connecting to host: '%s' at port: '%d'", connectionSettings.getHost(), connectionSettings.getPort()));
    }
    SftpClient client = clientFactory.createInstance(connectionSettings.getHost(), connectionSettings.getPort(),
                                                     connectionSettings.getPrngAlgorithm(), schedulerService, proxyConfig,
                                                     connectionSettings.isKexHeader(),
                                                     new FileBasedConfigProvider(securitySettings.getSshConfigOverride()));
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
    } catch (final SshException e) {
      client.disconnect();
      throw handleSshClientException(e);
    } catch (final IllegalStateException e) {
      client.disconnect();
      throw new SftpConnectionException(getErrorMessage(connectionSettings, e.getMessage()), e, FileError.INVALID_CREDENTIALS);
    } catch (Exception e) {
      client.disconnect();
      throw new SftpConnectionException(getErrorMessage(connectionSettings, e.getMessage()), e, FileError.CONNECTIVITY);
    }

    return new SftpFileSystemConnection(client, getWorkingDir(), lockFactory);
  }

  private SftpConnectionException handleSshClientException(SshException e) {

    String message = e.getMessage();
    int code = e.getDisconnectCode();

    if (code == SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE) {
      return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.INVALID_CREDENTIALS);
    } else if (code == 0) {
      if (message.contains("timeout")) {
        return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.CONNECTION_TIMEOUT);
      } else if (message.contains("Connection refused") || message.contains("refused the network connection")) {
        return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.CANNOT_REACH);
      } else if (message.contains("UnresolvedAddressException")) {
        return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.UNKNOWN_HOST);
      } else if (message.contains("Connection reset by peer") || message.contains("Connection reset")) {
        return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.CONNECTIVITY);
      } else {
        LOGGER.error(message);
        return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.UNKNOWN);
      }
    } else if (code == SSH2_DISCONNECT_KEY_EXCHANGE_FAILED) {
      return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.KEY_EXCHANGE_FAILED);
    } else if (code == 9) {
      return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.CANNOT_REACH);
    }
    LOGGER.error(message);
    return new SftpConnectionException(getErrorMessage(connectionSettings, message), e, FileError.DISCONNECTED);
  }

  @Override
  public void disconnect(SftpFileSystemConnection ftpFileSystem) {
    ftpFileSystem.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(SftpFileSystemConnection ftpFileSystem) {
    return ftpFileSystem.validateConnection();
  }

  public void setProxyConfig(SftpProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
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
