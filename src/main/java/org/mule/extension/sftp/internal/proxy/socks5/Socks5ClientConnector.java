/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy.socks5;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.common.util.Readable;

import org.ietf.jgss.GSSContext;

import org.mule.extension.sftp.internal.auth.AuthenticationHandler;
import org.mule.extension.sftp.internal.proxy.AbstractClientProxyConnector;
import org.mule.extension.sftp.internal.proxy.GssApiMechanisms;
import org.mule.extension.sftp.internal.proxy.ProtocolState;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.text.MessageFormat.format;

/**
 * A {@link AbstractClientProxyConnector} to connect through a SOCKS5 proxy.
 *
 * @see <a href="https://tools.ietf.org/html/rfc1928">RFC 1928</a>
 */
public class Socks5ClientConnector extends AbstractClientProxyConnector {

  // private static final byte SOCKS_VERSION_4 = 4;
  private static final byte SOCKS_VERSION_5 = 5;

  private static final byte SOCKS_CMD_CONNECT = 1;
  // private static final byte SOCKS5_CMD_BIND = 2;
  // private static final byte SOCKS5_CMD_UDP_ASSOCIATE = 3;

  // Address types

  private static final byte SOCKS_ADDRESS_IPv4 = 1;

  private static final byte SOCKS_ADDRESS_FQDN = 3;

  private static final byte SOCKS_ADDRESS_IPv6 = 4;

  // Reply codes

  private static final byte SOCKS_REPLY_SUCCESS = 0;

  private static final byte SOCKS_REPLY_FAILURE = 1;

  private static final byte SOCKS_REPLY_FORBIDDEN = 2;

  private static final byte SOCKS_REPLY_NETWORK_UNREACHABLE = 3;

  private static final byte SOCKS_REPLY_HOST_UNREACHABLE = 4;

  private static final byte SOCKS_REPLY_CONNECTION_REFUSED = 5;

  private static final byte SOCKS_REPLY_TTL_EXPIRED = 6;

  private static final byte SOCKS_REPLY_COMMAND_UNSUPPORTED = 7;

  private static final byte SOCKS_REPLY_ADDRESS_UNSUPPORTED = 8;
  private static final int SSH_DEFAULT_PORT = 22;


  private ProtocolState state;

  private AuthenticationHandler<Buffer, Buffer> authenticator;

  public ProtocolState getState() {
    return state;
  }

  public AuthenticationHandler<Buffer, Buffer> getAuthenticator() {
    return authenticator;
  }

  public GSSContext getContext() {
    return context;
  }

  private GSSContext context;

  private byte[] authenticationProposals;

  /**
   * Creates a new {@link Socks5ClientConnector}. The connector supports anonymous connections as well as username-password or
   * Kerberos5 (GSS-API) authentication.
   *
   * @param proxyAddress  of the proxy server we're connecting to
   * @param remoteAddress of the target server to connect to
   */
  public Socks5ClientConnector(InetSocketAddress proxyAddress,
                               InetSocketAddress remoteAddress) {
    this(proxyAddress, remoteAddress, null, null);
  }

  /**
   * Creates a new {@link Socks5ClientConnector}. The connector supports anonymous connections as well as username-password or
   * Kerberos5 (GSS-API) authentication.
   *
   * @param proxyAddress  of the proxy server we're connecting to
   * @param remoteAddress of the target server to connect to
   * @param proxyUser     to authenticate at the proxy with
   * @param proxyPassword to authenticate at the proxy with
   */
  public Socks5ClientConnector(InetSocketAddress proxyAddress,
                               InetSocketAddress remoteAddress,
                               String proxyUser, char[] proxyPassword) {
    super(proxyAddress, remoteAddress, proxyUser, proxyPassword);
    this.state = ProtocolState.NONE;
  }


  @Override
  public void sendClientProxyMetadata(ClientSession sshSession)
      throws Exception {
    init(sshSession);
    IoSession session = sshSession.getIoSession();
    // Send the initial request
    Buffer buffer = new ByteArrayBuffer(5, false);
    buffer.putByte(SOCKS_VERSION_5);
    context = getGSSContext(remoteAddress);
    authenticationProposals = getAuthenticationProposals();
    buffer.putByte((byte) authenticationProposals.length);
    buffer.putRawBytes(authenticationProposals);
    state = ProtocolState.INIT;
    session.writeBuffer(buffer).verify(getTimeout());
  }

  private byte[] getAuthenticationProposals() {
    byte[] proposals = new byte[3];
    int i = 0;
    proposals[i++] = SocksAuthenticationMethod.ANONYMOUS.getValue();
    proposals[i++] = SocksAuthenticationMethod.PASSWORD.getValue();
    if (context != null) {
      proposals[i++] = SocksAuthenticationMethod.GSSAPI.getValue();
    }
    if (i == proposals.length) {
      return proposals;
    }
    byte[] result = new byte[i];
    System.arraycopy(proposals, 0, result, 0, i);
    return result;
  }

  public void sendConnectInfo(IoSession session) throws Exception {
    GssApiMechanisms.closeContextSilently(context);

    byte[] rawAddress = getRawAddress(remoteAddress);
    byte[] remoteName = null;
    byte type;
    int length = 0;
    if (rawAddress == null) {
      remoteName = remoteAddress.getHostString().getBytes(US_ASCII);
      if (remoteName == null || remoteName.length == 0) {
        throw new IOException(format("Could not send remote address %s", remoteAddress));
      } else if (remoteName.length > 255) {
        // Should not occur; host names must not be longer than 255
        // US_ASCII characters. Internal error, no translation.
        throw new IOException(format(
                                     "Proxy host name too long for SOCKS (at most 255 characters): %s", //$NON-NLS-1$
                                     remoteAddress.getHostString()));
      }
      type = SOCKS_ADDRESS_FQDN;
      length = remoteName.length + 1;
    } else {
      length = rawAddress.length;
      type = length == 4 ? SOCKS_ADDRESS_IPv4 : SOCKS_ADDRESS_IPv6;
    }
    Buffer buffer = new ByteArrayBuffer(4 + length + 2, false);
    buffer.putByte(SOCKS_VERSION_5);
    buffer.putByte(SOCKS_CMD_CONNECT);
    buffer.putByte((byte) 0); // Reserved
    buffer.putByte(type);
    if (remoteName != null) {
      buffer.putByte((byte) remoteName.length);
      buffer.putRawBytes(remoteName);
    } else {
      buffer.putRawBytes(rawAddress);
    }
    int port = remoteAddress.getPort();
    if (port <= 0) {
      port = SSH_DEFAULT_PORT;
    }
    buffer.putByte((byte) ((port >> 8) & 0xFF));
    buffer.putByte((byte) (port & 0xFF));
    state = ProtocolState.CONNECTING;
    session.writeBuffer(buffer).verify(getTimeout());
  }

  public void doPasswordAuth(IoSession session) throws Exception {
    GssApiMechanisms.closeContextSilently(context);
    authenticator = new SocksBasicAuthentication(this);
    session.addCloseFutureListener(f -> close());
    startAuth(session);
  }

  public void doGssApiAuth(IoSession session) throws Exception {
    authenticator = new SocksGssApiAuthentication(this);
    session.addCloseFutureListener(f -> close());
    startAuth(session);
  }

  private void close() {
    AuthenticationHandler<?, ?> handler = authenticator;
    authenticator = null;
    if (handler != null) {
      handler.close();
    }
  }

  private void startAuth(IoSession session) throws Exception {
    Buffer buffer = null;
    try {
      authenticator.setParams(null);
      authenticator.start();
      buffer = authenticator.getToken();
      state = ProtocolState.AUTHENTICATING;
      if (buffer == null) {
        // Internal error; no translation
        throw new IOException(
                              "No data for proxy authentication with " //$NON-NLS-1$
                                  + proxyAddress);
      }
      session.writeBuffer(buffer).verify(getTimeout());
    } finally {
      if (buffer != null) {
        buffer.clear(true);
      }
    }
  }

  public void authStep(IoSession session, Buffer input) throws Exception {
    Buffer buffer = null;
    try {
      authenticator.setParams(input);
      authenticator.process();
      buffer = authenticator.getToken();
      if (buffer != null) {
        session.writeBuffer(buffer).verify(getTimeout());
      }
    } finally {
      if (buffer != null) {
        buffer.clear(true);
      }
    }
    if (authenticator.isDone()) {
      sendConnectInfo(session);
    }
  }

  public void establishConnection(Buffer data) throws Exception {
    byte reply = data.getByte();
    switch (reply) {
      case SOCKS_REPLY_SUCCESS:
        state = ProtocolState.CONNECTED;
        setDone(true);
        return;
      case SOCKS_REPLY_FAILURE:
        throw new IOException(format("SOCKS5 proxy %s: general failure", proxyAddress));
      case SOCKS_REPLY_FORBIDDEN:
        throw new IOException(format("SOCKS5 proxy %s: connection to %s not allowed by ruleset", proxyAddress, remoteAddress));
      case SOCKS_REPLY_NETWORK_UNREACHABLE:
        throw new IOException(format("SOCKS5 proxy %s: network unreachable %s", proxyAddress, remoteAddress));
      case SOCKS_REPLY_HOST_UNREACHABLE:
        throw new IOException(format("SOCKS5 proxy %s: host unreachable %s", proxyAddress, remoteAddress));
      case SOCKS_REPLY_CONNECTION_REFUSED:
        throw new IOException(format("SOCKS5 proxy %s: connection refused %s", proxyAddress, remoteAddress));
      case SOCKS_REPLY_TTL_EXPIRED:
        throw new IOException(format("TTL expired in SOCKS5 proxy connection %s", proxyAddress));
      case SOCKS_REPLY_COMMAND_UNSUPPORTED:
        throw new IOException(format("SOCKS5 proxy %s does not support CONNECT command", proxyAddress));
      case SOCKS_REPLY_ADDRESS_UNSUPPORTED:
        throw new IOException(format("SOCKS5 proxy %s does not support address type", proxyAddress));
      default:
        throw new IOException(format("Unspecified failure in SOCKS5 proxy connection %s", proxyAddress));
    }
  }

  @Override
  public void messageReceived(IoSession session, Readable buffer) throws Exception {
    try {
      // Dispatch according to protocol state
      ByteArrayBuffer data = new ByteArrayBuffer(buffer.available(),
                                                 false);
      data.putBuffer(buffer);
      data.compact();
      state.handleMessage(this, session, data);
    } catch (Exception e) {
      state = ProtocolState.FAILED;
      if (authenticator != null) {
        authenticator.close();
        authenticator = null;
      }
      try {
        setDone(false);
      } catch (Exception inner) {
        e.addSuppressed(inner);
      }
      throw e;
    }
  }

  public void versionCheck(byte version) throws Exception {
    if (version != SOCKS_VERSION_5) {
      throw new IOException(format("Expected SOCKS version 5, got %s", Integer.toString(version & 0xFF)));
    }
  }

  public SocksAuthenticationMethod getAuthMethod(byte value) {
    if (value != SocksAuthenticationMethod.NONE_ACCEPTABLE.getValue()) {
      for (byte proposed : authenticationProposals) {
        if (proposed == value) {
          for (SocksAuthenticationMethod method : SocksAuthenticationMethod
              .values()) {
            if (method.getValue() == value) {
              return method;
            }
          }
          break;
        }
      }
    }
    return SocksAuthenticationMethod.NONE_ACCEPTABLE;
  }

  private static byte[] getRawAddress(InetSocketAddress address) {
    InetAddress ipAddress = GssApiMechanisms.resolve(address);
    return ipAddress == null ? null : ipAddress.getAddress();
  }

  private static GSSContext getGSSContext(
                                          InetSocketAddress address) {
    if (!GssApiMechanisms.getSupportedMechanisms()
        .contains(GssApiMechanisms.KERBEROS_5)) {
      return null;
    }
    return GssApiMechanisms.createContext(GssApiMechanisms.KERBEROS_5,
                                          GssApiMechanisms.getCanonicalName(address));
  }

}
