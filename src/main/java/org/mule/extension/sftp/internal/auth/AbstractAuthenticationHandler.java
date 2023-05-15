/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import java.net.InetSocketAddress;

/**
 * Abstract base class for {@link AuthenticationHandler}s encapsulating basic common things.
 *
 * @param <ParameterType> defining the parameter type for the authentication
 * @param <TokenType>     defining the token type for the authentication
 */
public abstract class AbstractAuthenticationHandler<ParameterType, TokenType>
    implements AuthenticationHandler<ParameterType, TokenType> {

  /** The {@link InetSocketAddress} or the proxy to connect to. */
  protected InetSocketAddress proxy;

  /** The last set parameters. */
  protected ParameterType params;

  /** A flag telling whether this authentication is done. */
  private boolean done;

  /**
   * Creates a new {@link AbstractAuthenticationHandler} to authenticate with the given {@code proxy}.
   *
   * @param proxy the {@link InetSocketAddress} of the proxy to connect to
   */
  public AbstractAuthenticationHandler(InetSocketAddress proxy) {
    this.proxy = proxy;
  }

  @Override
  public final void setParams(ParameterType input) {
    params = input;
  }

  @Override
  public final boolean isDone() {
    return done;
  }

  @Override
  public final void setDone(boolean done) {
    this.done = done;
  }
}
