/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy.socks5;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.ietf.jgss.GSSContext;
import org.mule.extension.sftp.internal.auth.GssApiAuthentication;

import java.io.IOException;

import static java.text.MessageFormat.format;

/**
 * @see <a href="https://tools.ietf.org/html/rfc1961">RFC 1961</a>
 */
class SocksGssApiAuthentication
    extends GssApiAuthentication<Buffer, Buffer> {

  private static final byte SOCKS5_GSSAPI_VERSION = 1;

  private static final byte SOCKS5_GSSAPI_TOKEN = 1;

  private static final int SOCKS5_GSSAPI_FAILURE = 0xFF;

  private final Socks5ClientConnector socks5ClientConnector;

  public SocksGssApiAuthentication(Socks5ClientConnector socks5ClientConnector) {
    super(socks5ClientConnector.getProxyAddress());
    this.socks5ClientConnector = socks5ClientConnector;
  }

  @Override
  protected GSSContext createContext() throws Exception {
    return socks5ClientConnector.getContext();
  }

  @Override
  public Buffer getToken() throws Exception {
    if (token == null) {
      return null;
    }
    Buffer buffer = new ByteArrayBuffer(4 + token.length, false);
    buffer.putByte(SOCKS5_GSSAPI_VERSION);
    buffer.putByte(SOCKS5_GSSAPI_TOKEN);
    buffer.putByte((byte) ((token.length >> 8) & 0xFF));
    buffer.putByte((byte) (token.length & 0xFF));
    buffer.putRawBytes(token);
    return buffer;
  }

  @Override
  protected byte[] extractToken(Buffer input) throws Exception {
    if (socks5ClientConnector.getContext() == null) {
      return null;
    }
    int version = input.getUByte();
    if (version != SOCKS5_GSSAPI_VERSION) {
      throw new IOException(format("SOCKS5 proxy %s sent wrong GSS-API version number, expected 1, got %s",
                                   socks5ClientConnector.getProxyAddress(), Integer.toString(version)));
    }
    int msgType = input.getUByte();
    if (msgType == SOCKS5_GSSAPI_FAILURE) {
      throw new IOException(format("Cannot authenticate with GSS-API to SOCKS5 proxy %s",
                                   socks5ClientConnector.getProxyAddress()));
    } else if (msgType != SOCKS5_GSSAPI_TOKEN) {
      throw new IOException(format("SOCKS5 proxy %s sent unexpected GSS-API message type, expected 1, got %s",
                                   socks5ClientConnector.getProxyAddress(), Integer.toHexString(msgType & 0xFF)));
    }
    if (input.available() >= 2) {
      int length = (input.getUByte() << 8) + input.getUByte();
      if (input.available() >= length) {
        byte[] value = new byte[length];
        if (length > 0) {
          input.getRawBytes(value);
        }
        return value;
      }
    }
    throw new IOException(format("SOCKS5 proxy %s sent too short message", socks5ClientConnector.getProxyAddress()));
  }
}
