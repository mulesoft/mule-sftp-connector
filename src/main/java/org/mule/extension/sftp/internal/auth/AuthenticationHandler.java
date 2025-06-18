/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.sftp.internal.auth;

import java.io.Closeable;
import java.io.IOException;

import org.ietf.jgss.GSSException;

/**
 * An {@code AuthenticationHandler} encapsulates a possibly multi-step
 * authentication protocol. Intended usage:
 *
 * <pre>
 * setParams(something);
 * start();
 * sendToken(getToken());
 * while (!isDone()) {
 * 	setParams(receiveMessageAndExtractParams());
 * 	process();
 * 	Object t = getToken();
 * 	if (t != null) {
 * 		sendToken(t);
 * 	}
 * }
 * </pre>
 *
 * An {@code AuthenticationHandler} may be stateful and therefore is a
 * {@link java.io.Closeable}.
 *
 * @param <ParameterType>
 *            defining the parameter type for {@link #setParams(Object)}
 * @param <TokenType>
 *            defining the token type for {@link #getToken()}
 */
@SuppressWarnings("java:S112")
public interface AuthenticationHandler<ParameterType, TokenType>
    extends Closeable {

  /**
   * Produces the initial authentication token that can be then retrieved via
   * {@link #getToken()}.
   *
   * @throws GSSException
   *             if an error occurs
   */
  void start() throws GSSException;

  /**
   * Produces the next authentication token, if any.
   *
   * @throws IOException if an error occurs
   * @throws GSSException if an error occurs
   */
  void process() throws IOException, GSSException;

  /**
   * Sets the parameters for the next token generation via {@link #start()} or
   * {@link #process()}.
   *
   * @param input
   *            to set, may be {@code null}
   */
  void setParams(ParameterType input);

  /**
   * Retrieves the last token generated.
   *
   * @return the token, or {@code null} if there is none
   * @throws IOException
   *             if an error occurs
   */
  TokenType getToken() throws IOException;

  /**
   * Tells whether is authentication mechanism is done (successfully or
   * unsuccessfully).
   *
   * @return whether this authentication is done
   */
  boolean isDone();

  @Override
  void close();
}
