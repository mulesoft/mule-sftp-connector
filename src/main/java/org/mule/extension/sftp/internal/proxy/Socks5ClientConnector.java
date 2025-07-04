/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.sftp.internal.proxy;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;

import org.mule.extension.sftp.internal.auth.AuthenticationHandler;
import org.mule.extension.sftp.internal.auth.BasicAuthentication;
import org.mule.extension.sftp.internal.auth.GssApiAuthentication;
import org.mule.extension.sftp.internal.auth.GssApiMechanisms;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.Readable;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.BufferUtils;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * A {@link AbstractClientProxyConnector} to connect through a SOCKS5 proxy.
 *
 * @see <a href="https://tools.ietf.org/html/rfc1928">RFC 1928</a>
 */
@SuppressWarnings("java:S112")
public class Socks5ClientConnector extends AbstractClientProxyConnector {

  private static final byte SOCKS_VERSION_5 = 5;

  private static final byte SOCKS_CMD_CONNECT = 1;

  // Address types

  private static final byte SOCKS_ADDRESS_IPV4 = 1;

  private static final byte SOCKS_ADDRESS_FQDN = 3;

  private static final byte SOCKS_ADDRESS_IPV6 = 4;

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

  /**
   * Authentication methods for SOCKS5.
   *
   * @see <a href=
   *      "https://www.iana.org/assignments/socks-methods/socks-methods.xhtml">SOCKS
   *      Methods, IANA.org</a>
   */
  private enum SocksAuthenticationMethod {

    ANONYMOUS(0), GSSAPI(1), PASSWORD(2),
    // CHALLENGE_HANDSHAKE(3),
    // CHALLENGE_RESPONSE(5),
    // SSL(6),
    // NDS(7),
    // MULTI_AUTH(8),
    // JSON(9),
    NONE_ACCEPTABLE(0xFF);

    private final byte value;

    SocksAuthenticationMethod(int value) {
      this.value = (byte) value;
    }

    public byte getValue() {
      return value;
    }
  }

  private enum ProtocolState {
    NONE,

    INIT {

      @Override
      public void handleMessage(Socks5ClientConnector connector,
                                IoSession session, Buffer data)
          throws Exception {
        connector.versionCheck(data.getByte());
        SocksAuthenticationMethod authMethod = connector.getAuthMethod(
                                                                       data.getByte());
        switch (authMethod) {
          case ANONYMOUS:
            connector.sendConnectInfo(session);
            break;
          case PASSWORD:
            connector.doPasswordAuth(session);
            break;
          case GSSAPI:
            connector.doGssApiAuth(session);
            break;
          default:
            throw new IOException(
                                  format("Cannot authenticate to proxy {0}",
                                         connector.proxyAddress));
        }
      }
    },

    AUTHENTICATING {

      @Override
      public void handleMessage(Socks5ClientConnector connector,
                                IoSession session, Buffer data)
          throws Exception {
        connector.authStep(session, data);
      }
    },

    CONNECTING {

      @Override
      public void handleMessage(Socks5ClientConnector connector,
                                IoSession session, Buffer data)
          throws Exception {
        // Special case: when GSS-API authentication completes, the
        // client moves into CONNECTING as soon as the GSS context is
        // established and sends the connect request. This is per RFC
        // 1961. But for the server, RFC 1961 says it _should_ send an
        // empty token even if none generated when its server side
        // context is established. That means we may actually get an
        // empty token here. That message is 4 bytes long (and has
        // content 0x01, 0x01, 0x00, 0x00). We simply skip this message
        // if we get it here. If the server for whatever reason sends
        // back a "GSS failed" message (it shouldn't, at this point)
        // it will be two bytes 0x01 0xFF, which will fail the version
        // check.
        if (data.available() != 4) {
          connector.versionCheck(data.getByte());
          connector.establishConnection(data);
        }
      }
    },

    CONNECTED,

    FAILED;

    public void handleMessage(Socks5ClientConnector connector,
                              @SuppressWarnings("unused") IoSession session, Buffer data)
        throws Exception {
      throw new IOException(
                            format("Unexpected message received from SOCKS5 proxy {0}; client state {1}: {2}",
                                   connector.proxyAddress, this,
                                   BufferUtils.toHex(data.array())));
    }
  }

  private ProtocolState state;

  private AuthenticationHandler<Buffer, Buffer> authenticator;

  private GSSContext context;

  private byte[] authenticationProposals;

  /**
   * Creates a new {@link Socks5ClientConnector}. The connector supports
   * anonymous connections as well as username-password or Kerberos5 (GSS-API)
   * authentication.
   *
   * @param proxyAddress
   *            of the proxy server we're connecting to
   * @param remoteAddress
   *            of the target server to connect to
   */
  public Socks5ClientConnector(InetSocketAddress proxyAddress, InetSocketAddress remoteAddress) {
    this(proxyAddress, remoteAddress, null, null);
  }

  /**
   * Creates a new {@link Socks5ClientConnector}. The connector supports
   * anonymous connections as well as username-password or Kerberos5 (GSS-API)
   * authentication.
   *
   * @param proxyAddress
   *            of the proxy server we're connecting to
   * @param remoteAddress
   *            of the target server to connect to
   * @param proxyUser
   *            to authenticate at the proxy with
   * @param proxyPassword
   *            to authenticate at the proxy with
   */
  public Socks5ClientConnector(InetSocketAddress proxyAddress, InetSocketAddress remoteAddress,
                               String proxyUser, String proxyPassword) {
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

  private void sendConnectInfo(IoSession session) throws IOException {
    GssApiMechanisms.closeContextSilently(context);

    byte[] rawAddress = getRawAddress(remoteAddress);
    byte[] remoteName = null;
    byte type;
    int length = 0;
    if (rawAddress == null) {
      remoteName = remoteAddress.getHostString().getBytes(US_ASCII);
      if (remoteName == null || remoteName.length == 0) {
        throw new IOException(
                              format("Could not send remote address {0}",
                                     remoteAddress));
      } else if (remoteName.length > 255) {
        // Should not occur; host names must not be longer than 255
        // US_ASCII characters. Internal error, no translation.
        throw new IOException(format(
                                     "Proxy host name too long for SOCKS (at most 255 characters): {0}", //$NON-NLS-1$
                                     remoteAddress.getHostString()));
      }
      type = SOCKS_ADDRESS_FQDN;
      length = remoteName.length + 1;
    } else {
      length = rawAddress.length;
      type = length == 4 ? SOCKS_ADDRESS_IPV4 : SOCKS_ADDRESS_IPV6;
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
      port = SshConstants.DEFAULT_PORT;
    }
    buffer.putByte((byte) ((port >> 8) & 0xFF));
    buffer.putByte((byte) (port & 0xFF));
    state = ProtocolState.CONNECTING;
    session.writeBuffer(buffer).verify(getTimeout());
  }

  private void doPasswordAuth(IoSession session) throws Exception {
    GssApiMechanisms.closeContextSilently(context);
    authenticator = new SocksBasicAuthentication();
    session.addCloseFutureListener(f -> close());
    startAuth(session);
  }

  private void doGssApiAuth(IoSession session) throws Exception {
    authenticator = new SocksGssApiAuthentication();
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

  private void startAuth(IoSession session) throws IOException, GSSException {
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

  private void authStep(IoSession session, Buffer input) throws Exception {
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

  private void establishConnection(Buffer data) throws Exception {
    byte reply = data.getByte();
    switch (reply) {
      case SOCKS_REPLY_SUCCESS:
        state = ProtocolState.CONNECTED;
        setDone(true);
        return;
      case SOCKS_REPLY_FAILURE:
        throw new IOException(format(
                                     "SOCKS5 proxy {0}: general failure", proxyAddress));
      case SOCKS_REPLY_FORBIDDEN:
        throw new IOException(
                              format("SOCKS5 proxy {0}: connection to {1} not allowed by ruleset",
                                     proxyAddress, remoteAddress));
      case SOCKS_REPLY_NETWORK_UNREACHABLE:
        throw new IOException(
                              format("SOCKS5 proxy {0}: network unreachable {1}",
                                     proxyAddress, remoteAddress));
      case SOCKS_REPLY_HOST_UNREACHABLE:
        throw new IOException(
                              format("SOCKS5 proxy {0}: host unreachable {1}",
                                     proxyAddress, remoteAddress));
      case SOCKS_REPLY_CONNECTION_REFUSED:
        throw new IOException(
                              format("SOCKS5 proxy {0}: connection refused {1}",
                                     proxyAddress, remoteAddress));
      case SOCKS_REPLY_TTL_EXPIRED:
        throw new IOException(
                              format("TTL expired in SOCKS5 proxy connection {0}", proxyAddress));
      case SOCKS_REPLY_COMMAND_UNSUPPORTED:
        throw new IOException(
                              format("SOCKS5 proxy {0} does not support CONNECT command",
                                     proxyAddress));
      case SOCKS_REPLY_ADDRESS_UNSUPPORTED:
        throw new IOException(
                              format("SOCKS5 proxy {0} does not support address type",
                                     proxyAddress));
      default:
        throw new IOException(format("Unspecified failure in SOCKS5 proxy connection {0}", proxyAddress));
    }
  }

  @Override
  public void messageReceived(IoSession session, Readable buffer)
      throws Exception {
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

  private void versionCheck(byte version) throws IOException {
    if (version != SOCKS_VERSION_5) {
      throw new IOException(
                            format("Expected SOCKS version 5, got {0}",
                                   Integer.toString(version & 0xFF)));
    }
  }

  private SocksAuthenticationMethod getAuthMethod(byte value) {
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

  private static GSSContext getGSSContext(InetSocketAddress address) {
    if (!GssApiMechanisms.getSupportedMechanisms()
        .contains(GssApiMechanisms.KERBEROS_5)) {
      return null;
    }
    return GssApiMechanisms.createContext(GssApiMechanisms.KERBEROS_5,
                                          GssApiMechanisms.getCanonicalName(address));
  }

  /**
   * @see <a href="https://tools.ietf.org/html/rfc1929">RFC 1929</a>
   */
  private class SocksBasicAuthentication
      extends BasicAuthentication<Buffer, Buffer> {

    private static final byte SOCKS_BASIC_PROTOCOL_VERSION = 1;

    private static final byte SOCKS_BASIC_AUTH_SUCCESS = 0;

    public SocksBasicAuthentication() {
      super(proxyAddress, proxyUser, proxyPassword);
    }

    @Override
    public void process() throws IOException {
      // Retries impossible. RFC 1929 specifies that the server MUST
      // close the connection if authentication is unsuccessful.
      done = true;
      if (params.getByte() != SOCKS_BASIC_PROTOCOL_VERSION
          || params.getByte() != SOCKS_BASIC_AUTH_SUCCESS) {
        throw new IOException(format(
                                     "Authentication to SOCKS5 proxy {0} failed", proxy));
      }
    }

    @Override
    protected void askCredentials() {
      super.askCredentials();
      adjustTimeout();
    }

    @Override
    public Buffer getToken() throws IOException {
      if (done) {
        return null;
      }
      try {
        byte[] rawUser = user.getBytes(UTF_8);
        if (rawUser.length > 255) {
          throw new IOException(format(
                                       "User name for proxy {0} must be at most 255 bytes long, is {1} bytes: {2}", proxy,
                                       Integer.toString(rawUser.length), user));
        }

        if (password.length > 255) {
          throw new IOException(
                                format("Password for proxy {0} must be at most 255 bytes long, is {1} bytes",
                                       proxy, Integer.toString(password.length)));
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
        clearPassword();
        done = true;
      }
    }
  }

  /**
   * @see <a href="https://tools.ietf.org/html/rfc1961">RFC 1961</a>
   */
  private class SocksGssApiAuthentication
      extends GssApiAuthentication<Buffer, Buffer> {

    private static final byte SOCKS5_GSSAPI_VERSION = 1;

    private static final byte SOCKS5_GSSAPI_TOKEN = 1;

    private static final int SOCKS5_GSSAPI_FAILURE = 0xFF;

    public SocksGssApiAuthentication() {
      super(proxyAddress);
    }

    @Override
    protected GSSContext createContext() {
      return context;
    }

    @Override
    public Buffer getToken() {
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
    protected byte[] extractToken(Buffer input) throws IOException {
      if (context == null) {
        return new byte[0];
      }
      int version = input.getUByte();
      if (version != SOCKS5_GSSAPI_VERSION) {
        throw new IOException(
                              format("SOCKS5 proxy {0} sent wrong GSS-API version number, expected 1, got {1}", remoteAddress,
                                     Integer.toString(version)));
      }
      int msgType = input.getUByte();
      if (msgType == SOCKS5_GSSAPI_FAILURE) {
        throw new IOException(format("Cannot authenticate with GSS-API to SOCKS5 proxy {0}", remoteAddress));
      } else if (msgType != SOCKS5_GSSAPI_TOKEN) {
        throw new IOException(format(
                                     "Connection failed to {0} with message type 0x{1}",
                                     remoteAddress, Integer.toHexString(msgType & 0xFF)));
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
      throw new IOException(
                            format("SOCKS5 proxy {0} sent too short message",
                                   remoteAddress));
    }
  }
}
