/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import static java.lang.String.format;

import org.mule.extension.sftp.internal.proxy.GssApiMechanisms;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.ietf.jgss.GSSContext;

/**
 * An abstract implementation of a GSS-API multi-round authentication.
 *
 * @param <P> defining the parameter type for the authentication
 * @param <T> defining the token type for the authentication
 */
public abstract class GssApiAuthentication<P, T>
    extends AbstractAuthenticationHandler<P, T> {

  private GSSContext context;

  /** The last token generated. */
  protected byte[] token;

  /**
   * Creates a new {@link GssApiAuthentication} to authenticate with the given {@code proxy}.
   *
   * @param proxy the {@link InetSocketAddress} of the proxy to connect to
   */
  public GssApiAuthentication(InetSocketAddress proxy) {
    super(proxy);
  }

  @Override
  public void close() {
    GssApiMechanisms.closeContextSilently(context);
    context = null;
    setDone(true);
  }

  @Override
  public final void start() throws Exception {
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
  public final void process() throws Exception {
    if (context == null) {
      throw new IOException(format("Cannot authenticate to proxy %s", proxy));
    }
    try {
      byte[] received = extractToken(params);
      this.token = context.initSecContext(received, 0, received.length);
      if (context.isEstablished()) {
        this.context.dispose();
        context = null;
        setDone(true);
      }
    } catch (Exception e) {
      close();
      throw e;
    }
  }

  /**
   * Creates the {@link GSSContext} to use.
   *
   * @return a fresh {@link GSSContext} to use
   * @throws Exception if the context cannot be created
   */
  protected abstract GSSContext createContext() throws Exception;

  /**
   * Extracts the token from the last set parameters.
   *
   * @param input to extract the token from
   * @return the extracted token, or {@code null} if none
   * @throws Exception if an error occurs
   */
  protected abstract byte[] extractToken(P input)
      throws Exception;
}
