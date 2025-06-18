/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import static java.lang.String.format;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * An abstract implementation of a GSS-API multi-round authentication.
 *
 * @param <ParameterType>
 *            defining the parameter type for the authentication
 * @param <TokenType>
 *            defining the token type for the authentication
 */
@SuppressWarnings("java:S112")
public abstract class GssApiAuthentication<ParameterType, TokenType>
    extends AbstractAuthenticationHandler<ParameterType, TokenType> {

  private GSSContext context;

  /** The last token generated. */
  protected byte[] token;

  /**
   * Creates a new {@link org.mule.extension.sftp.internal.auth.GssApiAuthentication} to authenticate with the given
   * {@code proxy}.
   *
   * @param proxy
   *            the {@link java.net.InetSocketAddress} of the proxy to connect to
   */
  protected GssApiAuthentication(InetSocketAddress proxy) {
    super(proxy);
  }

  @Override
  public void close() {
    GssApiMechanisms.closeContextSilently(context);
    context = null;
    done = true;
  }

  @Override
  public final void start() throws GSSException {
    try {
      context = createContext();
      context.requestMutualAuth(true);
      context.requestConf(false);
      context.requestInteg(false);
      byte[] empty = new byte[0];
      token = context.initSecContext(empty, 0, 0);
    } catch (Exception e) {
      close();
      throw e;
    }
  }

  @Override
  public final void process() throws IOException, GSSException {
    if (context == null) {
      throw new IOException(format("Cannot authenticate to proxy %s", proxy));
    }
    try {
      byte[] received = extractToken(params);
      token = context.initSecContext(received, 0, received.length);
      checkDone();
    } catch (Exception e) {
      close();
      throw e;
    }
  }

  private void checkDone() throws GSSException {
    done = context.isEstablished();
    if (done) {
      context.dispose();
      context = null;
    }
  }

  /**
   * Creates the {@link org.ietf.jgss.GSSContext} to use.
   *
   * @return a fresh {@link org.ietf.jgss.GSSContext} to use
   */
  protected abstract GSSContext createContext();

  /**
   * Extracts the token from the last set parameters.
   *
   * @param input
   *            to extract the token from
   * @return the extracted token, or {@code null} if none
   * @throws IOException
   *             if an error occurs
   */
  protected abstract byte[] extractToken(ParameterType input)
      throws IOException;
}
