/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/**
 * An abstract implementation of a username-password authentication. It can be given an initial known username-password pair; if
 * so, this will be tried first. Subsequent rounds will then try to obtain a user name and password via the global
 * {@link Authenticator}.
 *
 * @param <P> defining the parameter type for the authentication
 * @param <T> defining the token type for the authentication
 */
public abstract class BasicAuthentication<P, T>
    extends AbstractAuthenticationHandler<P, T> {

  private static final String SSH_SCHEME = "\"ssh:\" hier-part";
  /** The current user name. */
  protected String user;

  /** The current password. */
  protected byte[] password;

  /**
   * Creates a new {@link BasicAuthentication} to authenticate with the given {@code proxy}.
   *
   * @param proxy           {@link InetSocketAddress} of the proxy to connect to
   * @param initialUser     initial user name to try; may be {@code null}
   * @param initialPassword initial password to try, may be {@code null}
   */
  protected BasicAuthentication(InetSocketAddress proxy, String initialUser,
                                char[] initialPassword) {
    super(proxy);
    this.user = initialUser;
    this.password = convert(initialPassword);
  }

  private byte[] convert(char[] pass) {
    if (pass == null) {
      return new byte[0];
    }
    ByteBuffer bytes = UTF_8.encode(CharBuffer.wrap(pass));
    byte[] pwd = new byte[bytes.remaining()];
    bytes.get(pwd);
    if (bytes.hasArray()) {
      Arrays.fill(bytes.array(), (byte) 0);
    }
    Arrays.fill(pass, '\000');
    return pwd;
  }

  /**
   * Clears the {@link #password}.
   */
  protected synchronized void clearPassword() {
    if (password != null) {
      Arrays.fill(password, (byte) 0);
    }
    password = new byte[0];
  }

  @Override
  public final void close() {
    clearPassword();
    setDone(true);
  }

  @Override
  public final void start() throws Exception {
    if ((user != null && !user.isEmpty())
        || (password != null && password.length > 0)) {
      return;
    }
    askCredentials();
  }

  @Override
  public void process() throws Exception {
    askCredentials();
  }

  /**
   * Asks for credentials via the global {@link Authenticator}.
   */
  protected void askCredentials() throws Exception {
    clearPassword();
    PasswordAuthentication auth = AccessController.doPrivileged(
                                                                (PrivilegedAction<PasswordAuthentication>) () -> Authenticator
                                                                    .requestPasswordAuthentication(proxy.getHostString(),
                                                                                                   proxy.getAddress(),
                                                                                                   proxy.getPort(),
                                                                                                   SSH_SCHEME,
                                                                                                   "Proxy password",
                                                                                                   "Basic", //$NON-NLS-1$
                                                                                                   null, RequestorType.PROXY));
    if (auth == null) {
      user = "";
      throw new CancellationException("Authentication canceled: no password");
    }
    user = auth.getUserName();
    password = convert(auth.getPassword());
  }
}
