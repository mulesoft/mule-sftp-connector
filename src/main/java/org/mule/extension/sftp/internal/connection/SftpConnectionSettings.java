/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static org.mule.runtime.api.meta.model.display.PathModel.Location.EMBEDDED;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;

import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.security.Secret;
import org.mule.sdk.api.annotation.semantics.security.Username;

import java.util.Objects;

/**
 * Groups SFTP connection settings
 *
 * @since 1.0
 */
public final class SftpConnectionSettings {

  /**
   * The FTP server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
   */
  @Parameter
  @Placement(order = 1)
  @Host
  private String host;

  /**
   * The port number of the SFTP server to connect on
   */
  @Parameter
  @Optional(defaultValue = "22")
  @Placement(order = 2)
  @Port
  private int port = 22;

  /**
   * Username for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  @Username
  protected String username;

  /**
   * Password for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Password
  @Placement(order = 4)
  private String password;

  /**
   * The passphrase (password) for the identityFile if required. Notice that this parameter is ignored if {@link #identityFile} is
   * not provided
   */
  @Parameter
  @Optional
  @Password
  @Placement(order = 6)
  @Summary("The passphrase (password) for the identityFile, if configured")
  @Secret
  private String passphrase;

  /**
   * An identityFile location for a PKI private key.
   */
  @Parameter
  @Optional
  @Placement(order = 5)
  @Path(type = FILE, location = EMBEDDED)
  @Secret
  private String identityFile;


  @Parameter
  @Optional(defaultValue = "AUTOSELECT")
  @Summary("The Pseudo Random Generator Algorithm to use")
  @Placement(order = 7)
  @DisplayName("PRNG Algorithm")
  private PRNGAlgorithm prngAlgorithm;

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public String getIdentityFile() {
    return identityFile;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  public void setIdentityFile(String identityFile) {
    this.identityFile = identityFile;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public PRNGAlgorithm getPrngAlgorithm() {
    return prngAlgorithm;
  }

  public void setPrngAlgorithm(PRNGAlgorithm prngAlgorithm) {
    this.prngAlgorithm = prngAlgorithm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SftpConnectionSettings that = (SftpConnectionSettings) o;
    return port == that.port &&
        Objects.equals(host, that.host) &&
        Objects.equals(username, that.username) &&
        Objects.equals(password, that.password) &&
        Objects.equals(passphrase, that.passphrase) &&
        Objects.equals(identityFile, that.identityFile) &&
        prngAlgorithm == that.prngAlgorithm;
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, username, password, passphrase, identityFile, prngAlgorithm);
  }
}
