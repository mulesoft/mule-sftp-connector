/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import java.util.Objects;

/**
 * A Proxy configuration for the SFTP connector.
 *
 * @since 3.9
 */
public class SftpProxyConfig {

  public enum Protocol {
    HTTP, SOCKS4, SOCKS5
  };

  @Parameter
  private String host;

  @Parameter
  private Integer port;

  @Parameter
  @Optional
  private String username;

  @Parameter
  @Optional
  @Password
  private String password;

  @Parameter
  private Protocol protocol;

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SftpProxyConfig that = (SftpProxyConfig) o;
    return Objects.equals(host, that.host) &&
        Objects.equals(port, that.port) &&
        Objects.equals(username, that.username) &&
        Objects.equals(password, that.password) &&
        protocol == that.protocol;
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, username, password, protocol);
  }

}
