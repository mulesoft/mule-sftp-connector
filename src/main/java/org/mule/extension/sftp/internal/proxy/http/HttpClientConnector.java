/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy.http;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.lang.String.format;

import org.mule.extension.sftp.internal.proxy.AbstractClientProxyConnector;
import org.mule.extension.sftp.internal.proxy.AuthenticationChallenge;
import org.mule.extension.sftp.internal.proxy.HttpParser;
import org.mule.extension.sftp.internal.proxy.StatusLine;

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

/**
 * Simple HTTP proxy connector using Basic Authentication.
 */
public class HttpClientConnector extends AbstractClientProxyConnector {

  private static final String HTTP_HEADER_PROXY_AUTHENTICATION = "Proxy-Authentication:"; //$NON-NLS-1$

  private static final String HTTP_HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization:"; //$NON-NLS-1$

  private static final String ERROR_MSG_UNEXPECTED_RESPONSE = "Unexpected HTTP proxy response from %s: %s";

  private HttpAuthenticationHandler basic;

  private HttpAuthenticationHandler negotiate;

  private List<HttpAuthenticationHandler> availableAuthentications;

  private Iterator<HttpAuthenticationHandler> clientAuthentications;

  private HttpAuthenticationHandler authenticator;

  private boolean ongoing;

  /**
   * Creates a new {@link HttpClientConnector}. The connector supports anonymous proxy connections as well as Basic and Negotiate
   * authentication.
   *
   * @param proxyAddress  of the proxy server we're connecting to
   * @param remoteAddress of the target server to connect to
   */
  public HttpClientConnector(InetSocketAddress proxyAddress,
                             InetSocketAddress remoteAddress) {
    this(proxyAddress, remoteAddress, null, null);
  }

  /**
   * Creates a new {@link HttpClientConnector}. The connector supports anonymous proxy connections as well as Basic and Negotiate
   * authentication. If a user name and password are given, the connector tries pre-emptive Basic authentication.
   *
   * @param proxyAddress  of the proxy server we're connecting to
   * @param remoteAddress of the target server to connect to
   * @param proxyUser     to authenticate at the proxy with
   * @param proxyPassword to authenticate at the proxy with
   */
  public HttpClientConnector(InetSocketAddress proxyAddress,
                             InetSocketAddress remoteAddress, String proxyUser,
                             char[] proxyPassword) {
    super(proxyAddress, remoteAddress, proxyUser, proxyPassword);
    basic = new HttpBasicAuthentication(this);
    negotiate = new NegotiateAuthentication(this);
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
      throws Exception {
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
    } catch (Exception e) {
      ongoing = false;
      throw e;
    }
  }

  private void send(StringBuilder msg, IoSession session) throws Exception {
    byte[] data = eol(msg).toString().getBytes(US_ASCII);
    Buffer buffer = new ByteArrayBuffer(data.length, false);
    buffer.putRawBytes(data);
    session.writeBuffer(buffer).verify(getTimeout());
  }

  private StringBuilder connect() {
    StringBuilder msg = new StringBuilder();
    // Persistent connections are the default in HTTP 1.1 (see RFC 2616),
    // but let's be explicit.
    return msg.append(format(
                             "CONNECT HOST %s:%s HTTP/1.1%nProxy-Connection: keep-alive%nConnection: keep-alive%n",
                             // $NON-NLS-1$
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

  public void messageReceived(IoSession session, Readable buffer) throws Exception {
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
      throw new IOException(format(ERROR_MSG_UNEXPECTED_RESPONSE, proxyAddress.getAddress(),
                                   Integer.toString(proxyAddress.getPort()))); // $NON-NLS-1$
    }
    try {
      StatusLine status = HttpParser.parseStatusLine(reply.get(0));
      if (!ongoing) {
        throw new IOException(format(ERROR_MSG_UNEXPECTED_RESPONSE, proxyAddress,
                                     Integer.toString(status.getResultCode()), status.getReason()));
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
          List<AuthenticationChallenge> challenges = HttpParser.getAuthenticationHeaders(reply, HTTP_HEADER_PROXY_AUTHENTICATION);
          authenticator = selectProtocol(challenges, authenticator);
          if (authenticator == null) {
            throw new IOException(format("Cannot authenticate to proxy %s", proxyAddress));
          }
          String token = authenticator.getToken();
          if (token == null) {
            throw new IOException(format("Cannot authenticate to proxy %s", proxyAddress));
          }
          send(authenticate(connect(), token), session);
          break;
        default:
          throw new IOException(format("HTTP Proxy connection to %s failed with code %s: %s", proxyAddress,
                                       Integer.toString(status.getResultCode()), status.getReason()));
      }
    } catch (HttpParser.ParseException e) {
      throw new IOException(format(ERROR_MSG_UNEXPECTED_RESPONSE, proxyAddress, reply.get(0)), e);
    }
  }

  private HttpAuthenticationHandler selectProtocol(
                                                   List<AuthenticationChallenge> challenges,
                                                   HttpAuthenticationHandler current)
      throws Exception {
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
}
