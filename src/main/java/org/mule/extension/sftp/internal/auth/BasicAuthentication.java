/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
 * An abstract implementation of a username-password authentication. It can be
 * given an initial known username-password pair; if so, this will be tried
 * first. Subsequent rounds will then try to obtain a user name and password via
 * the global {@link java.net.Authenticator}.
 *
 * @param <ParameterType>
 *            defining the parameter type for the authentication
 * @param <TokenType>
 *            defining the token type for the authentication
 */
public abstract class BasicAuthentication<ParameterType, TokenType>
    extends AbstractAuthenticationHandler<ParameterType, TokenType> {

  /** The current user name. */
  protected String user;

  /** The current password. */
  protected byte[] password;

  /**
   * Creates a new {@link org.mule.extension.sftp.internal.auth.BasicAuthentication} to authenticate with the given
   * {@code proxy}.
   *
   * @param proxy
   *            {@link java.net.InetSocketAddress} of the proxy to connect to
   * @param initialUser
   *            initial user name to try; may be {@code null}
   * @param initialPassword
   *            initial password to try, may be {@code null}
   */
  public BasicAuthentication(InetSocketAddress proxy, String initialUser,
                             char[] initialPassword) {
    super(proxy);
    this.user = initialUser;
    this.password = convert(initialPassword);
  }

  @SuppressWarnings("ByteBufferBackingArray")
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
  protected void clearPassword() {
    if (password != null) {
      Arrays.fill(password, (byte) 0);
    }
    password = new byte[0];
  }

  @Override
  public final void close() {
    clearPassword();
    done = true;
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
   * Asks for credentials via the global {@link java.net.Authenticator}.
   */
  protected void askCredentials() {
    clearPassword();
    PasswordAuthentication auth = AccessController.doPrivileged(
                                                                (PrivilegedAction<PasswordAuthentication>) () -> Authenticator
                                                                    .requestPasswordAuthentication(proxy.getHostString(),
                                                                                                   proxy.getAddress(),
                                                                                                   proxy.getPort(),
                                                                                                   "sftp",
                                                                                                   "Proxy password", "Basic", //$NON-NLS-1$
                                                                                                   null, RequestorType.PROXY));
    if (auth == null) {
      user = ""; //$NON-NLS-1$
      throw new CancellationException("SSH authentication canceled: no password given");
    }
    user = auth.getUserName();
    password = convert(auth.getPassword());
  }
}
