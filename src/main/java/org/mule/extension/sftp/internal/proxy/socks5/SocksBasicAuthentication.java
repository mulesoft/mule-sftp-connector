/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy.socks5;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.lang.String.format;

import org.mule.extension.sftp.internal.auth.BasicAuthentication;

import java.io.IOException;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

/**
 * @see <a href="https://tools.ietf.org/html/rfc1929">RFC 1929</a>
 */
class SocksBasicAuthentication
    extends BasicAuthentication<Buffer, Buffer> {

  private static final byte SOCKS_BASIC_PROTOCOL_VERSION = 1;

  private static final byte SOCKS_BASIC_AUTH_SUCCESS = 0;

  private final Socks5ClientConnector socks5ClientConnector;

  public SocksBasicAuthentication(Socks5ClientConnector socks5ClientConnector) {
    super(socks5ClientConnector.getProxyAddress(), socks5ClientConnector.getProxyUser(),
          socks5ClientConnector.getProxyPassword());
    this.socks5ClientConnector = socks5ClientConnector;
  }

  @Override
  public void process() throws Exception {
    // Retries impossible. RFC 1929 specifies that the server MUST
    // close the connection if authentication is unsuccessful.
    socks5ClientConnector.setDone(true);
    if (params.getByte() != SOCKS_BASIC_PROTOCOL_VERSION
        || params.getByte() != SOCKS_BASIC_AUTH_SUCCESS) {
      throw new IOException(format("Authentication to SOCKS5 proxy %s failed", proxy));
    }
  }

  @Override
  protected void askCredentials() throws Exception {
    super.askCredentials();
    socks5ClientConnector.adjustTimeout();
  }

  @Override
  public Buffer getToken() throws Exception {
    if (socks5ClientConnector.isDone()) {
      return null;
    }
    try {
      byte[] rawUser = user.getBytes(UTF_8);
      if (rawUser.length > 255) {
        throw new IOException(format("User name for proxy %s must be at most 255 bytes long, is %s bytes: %s", proxy,
                                     Integer.toString(rawUser.length), user));
      }

      if (password.length > 255) {
        throw new IOException(format("Password for proxy %s must be at most 255 bytes long, is %s bytes", proxy,
                                     Integer.toString(password.length)));
      }
      ByteArrayBuffer buffer = new ByteArrayBuffer(
                                                   3 + rawUser.length + password.length, false);
      buffer.putByte(SOCKS_BASIC_PROTOCOL_VERSION);
      buffer.putByte((byte) rawUser.length);
      buffer.putRawBytes(rawUser);
      buffer.putByte((byte) password.length);
      buffer.putRawBytes(password);
      return buffer;
    } finally {
      socks5ClientConnector.clearPassword();
      socks5ClientConnector.setDone(true);
    }
  }
}
