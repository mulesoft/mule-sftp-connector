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
import org.mule.extension.sftp.internal.exception.ProxyConnectionException;
import org.mule.runtime.core.api.util.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.Readable;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * Simple HTTP proxy connector using Basic Authentication.
 */
public class HttpClientConnector extends AbstractClientProxyConnector {

  private static final String HTTP_HEADER_PROXY_AUTHENTICATION = "Proxy-Authentication:"; //$NON-NLS-1$

  private static final String HTTP_HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization:"; //$NON-NLS-1$

  private HttpAuthenticationHandler basic;

  private HttpAuthenticationHandler negotiate;

  private List<HttpAuthenticationHandler> availableAuthentications;

  private Iterator<HttpAuthenticationHandler> clientAuthentications;

  private HttpAuthenticationHandler authenticator;

  private boolean ongoing;

  private static final String UNEXPECTED_PROXY_RESPONSE_MSG = "Unexpected HTTP proxy response from {0}: {1}";


  /**
   * Creates a new {@link HttpClientConnector}. The connector supports
   * anonymous proxy connections as well as Basic and Negotiate
   * authentication.
   *
   * @param proxyAddress
   *            of the proxy server we're connecting to
   * @param remoteAddress
   *            of the target server to connect to
   */
  public HttpClientConnector(InetSocketAddress proxyAddress, InetSocketAddress remoteAddress) {
    this(proxyAddress, remoteAddress, null, null);
  }

  /**
   * Creates a new {@link HttpClientConnector}. The connector supports
   * anonymous proxy connections as well as Basic and Negotiate
   * authentication. If a user name and password are given, the connector
   * tries pre-emptive Basic authentication.
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
  public HttpClientConnector(InetSocketAddress proxyAddress, InetSocketAddress remoteAddress, String proxyUser,
                             String proxyPassword) {
    super(proxyAddress, remoteAddress, proxyUser, proxyPassword);
    basic = new HttpBasicAuthentication();
    negotiate = new NegotiateAuthentication();
    availableAuthentications = new ArrayList<>(2);
    availableAuthentications.add(negotiate);
    availableAuthentications.add(basic);
    clientAuthentications = availableAuthentications.iterator();
  }

  private void close() {
    HttpAuthenticationHandler current = authenticator;
    authenticator = null;
    if (current != null) {
      current.close();
    }
  }

  @Override
  public void sendClientProxyMetadata(ClientSession sshSession)
      throws ProxyConnectionException, IOException, GSSException {
    init(sshSession);
    IoSession session = sshSession.getIoSession();
    session.addCloseFutureListener(f -> close());
    StringBuilder msg = connect();
    if ((proxyUser != null && !proxyUser.isEmpty())
        || (proxyPassword != null && proxyPassword.length > 0)) {
      authenticator = basic;
      basic.setParams(null);
      basic.start();
      msg = authenticate(msg, basic.getToken());
      clearPassword();
      proxyUser = null;
    }
    ongoing = true;
    try {
      send(msg, session);
    } catch (ProxyConnectionException e) {
      ongoing = false;
      throw e;
    }
  }

  private void send(StringBuilder msg, IoSession session) throws ProxyConnectionException {
    byte[] data = eol(msg).toString().getBytes(US_ASCII);
    Buffer buffer = new ByteArrayBuffer(data.length, false);
    buffer.putRawBytes(data);
    try {
      session.writeBuffer(buffer).verify(getTimeout());
    } catch (Exception e) {
      throw new ProxyConnectionException("Failed to send data through proxy", e);
    }
  }

  private StringBuilder connect() {
    StringBuilder msg = new StringBuilder();
    // Persistent connections are the default in HTTP 1.1 (see RFC 2616),
    // but let's be explicit.
    return msg.append(format(
                             "CONNECT {0}:{1} HTTP/1.1\r\nProxy-Connection: keep-alive\r\nConnection: keep-alive\r\nHost: {0}:{1}\r\n", //$NON-NLS-1$
                             remoteAddress.getHostString(),
                             Integer.toString(remoteAddress.getPort())));
  }

  private StringBuilder authenticate(StringBuilder msg, String token) {
    msg.append(HTTP_HEADER_PROXY_AUTHORIZATION).append(' ').append(token);
    return eol(msg);
  }

  private StringBuilder eol(StringBuilder msg) {
    return msg.append('\r').append('\n');
  }

  @Override
  public void messageReceived(IoSession session, Readable buffer)
      throws Exception {
    try {
      int length = buffer.available();
      byte[] data = new byte[length];
      buffer.getRawBytes(data, 0, length);
      String[] reply = new String(data, US_ASCII)
          .split("\r\n"); //$NON-NLS-1$
      handleMessage(session, Arrays.asList(reply));
    } catch (Exception e) {
      if (authenticator != null) {
        authenticator.close();
        authenticator = null;
      }
      ongoing = false;
      try {
        setDone(false);
      } catch (Exception inner) {
        e.addSuppressed(inner);
      }
      throw e;
    }
  }

  private void handleMessage(IoSession session, List<String> reply)
      throws Exception {
    if (reply.isEmpty() || reply.get(0).isEmpty()) {
      throw new IOException(
                            format(UNEXPECTED_PROXY_RESPONSE_MSG,
                                   proxyAddress, "<empty>")); //$NON-NLS-1$
    }
    try {
      StatusLine status = HttpParser.parseStatusLine(reply.get(0));
      if (!ongoing) {
        throw new IOException(format(
                                     UNEXPECTED_PROXY_RESPONSE_MSG, proxyAddress,
                                     Integer.toString(status.getResultCode()),
                                     status.getReason()));
      }
      switch (status.getResultCode()) {
        case HttpURLConnection.HTTP_OK:
          if (authenticator != null) {
            authenticator.close();
          }
          authenticator = null;
          ongoing = false;
          setDone(true);
          break;
        case HttpURLConnection.HTTP_PROXY_AUTH:
          List<AuthenticationChallenge> challenges = HttpParser
              .getAuthenticationHeaders(reply,
                                        HTTP_HEADER_PROXY_AUTHENTICATION);
          authenticator = selectProtocol(challenges, authenticator);
          if (authenticator == null) {
            throw new IOException(
                                  format("Cannot authenticate to proxy {0}",
                                         proxyAddress));
          }
          String token = authenticator.getToken();
          if (token == null) {
            throw new IOException(
                                  format("Cannot authenticate to proxy {0}",
                                         proxyAddress));
          }
          send(authenticate(connect(), token), session);
          break;
        default:
          throw new IOException(format("HTTP Proxy connection to {0} failed with code {1}: {2}",
                                       proxyAddress, Integer.toString(status.getResultCode()),
                                       status.getReason()));
      }
    } catch (HttpParser.ParseException e) {
      throw new IOException(
                            format(UNEXPECTED_PROXY_RESPONSE_MSG,
                                   proxyAddress, reply.get(0)),
                            e);
    }
  }

  private HttpAuthenticationHandler selectProtocol(
                                                   List<AuthenticationChallenge> challenges,
                                                   HttpAuthenticationHandler current)
      throws IOException, GSSException {
    if (current != null && !current.isDone()) {
      AuthenticationChallenge challenge = getByName(challenges,
                                                    current.getName());
      if (challenge != null) {
        current.setParams(challenge);
        current.process();
        return current;
      }
    }
    if (current != null) {
      current.close();
    }
    while (clientAuthentications.hasNext()) {
      HttpAuthenticationHandler next = clientAuthentications.next();
      if (!next.isDone()) {
        AuthenticationChallenge challenge = getByName(challenges,
                                                      next.getName());
        if (challenge != null) {
          next.setParams(challenge);
          next.start();
          return next;
        }
      }
    }
    return null;
  }

  private AuthenticationChallenge getByName(
                                            List<AuthenticationChallenge> challenges,
                                            String name) {
    return challenges.stream()
        .filter(c -> c.getMechanism().equalsIgnoreCase(name))
        .findFirst().orElse(null);
  }

  private interface HttpAuthenticationHandler
      extends AuthenticationHandler<AuthenticationChallenge, String> {

    public String getName();
  }

  /**
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  @SuppressWarnings("java:S112")
  private class HttpBasicAuthentication
      extends BasicAuthentication<AuthenticationChallenge, String>
      implements HttpAuthenticationHandler {

    private boolean asked;

    public HttpBasicAuthentication() {
      super(proxyAddress, proxyUser, proxyPassword);
    }

    @Override
    public String getName() {
      return "Basic"; //$NON-NLS-1$
    }

    @Override
    protected void askCredentials() {
      // We ask only once.
      if (asked) {
        throw new IllegalStateException(
                                        "Basic auth: already asked user for password"); //$NON-NLS-1$
      }
      asked = true;
      super.askCredentials();
      done = true;
    }

    @Override
    public String getToken() throws IOException {
      if (user.indexOf(':') >= 0) {
        throw new IOException(format(
                                     "HTTP Proxy connection to {0} failed with code {1}: {2}", proxy, user));
      }
      byte[] rawUser = user.getBytes(UTF_8);
      byte[] toEncode = new byte[rawUser.length + 1 + password.length];
      System.arraycopy(rawUser, 0, toEncode, 0, rawUser.length);
      toEncode[rawUser.length] = ':';
      System.arraycopy(password, 0, toEncode, rawUser.length + 1,
                       password.length);
      Arrays.fill(password, (byte) 0);
      String result = Base64.encodeBytes(toEncode);
      Arrays.fill(toEncode, (byte) 0);
      return getName() + ' ' + result;
    }

  }

  /**
   * @see <a href="https://tools.ietf.org/html/rfc4559">RFC 4559</a>
   */
  private class NegotiateAuthentication
      extends GssApiAuthentication<AuthenticationChallenge, String>
      implements HttpAuthenticationHandler {

    public NegotiateAuthentication() {
      super(proxyAddress);
    }

    @Override
    public String getName() {
      return "Negotiate"; //$NON-NLS-1$
    }

    @Override
    public String getToken() throws IOException {
      return getName() + ' ' + Base64.encodeBytes(token);
    }

    @Override
    protected GSSContext createContext() {
      return GssApiMechanisms.createContext(GssApiMechanisms.SPNEGO,
                                            GssApiMechanisms.getCanonicalName(proxyAddress));
    }

    @Override
    protected byte[] extractToken(AuthenticationChallenge input) {
      String received = input.getToken();
      if (received == null) {
        return new byte[0];
      }
      return Base64.decode(received);
    }

  }
}
