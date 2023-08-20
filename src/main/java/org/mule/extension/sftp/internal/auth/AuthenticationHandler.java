/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.auth;

import java.io.Closeable;

/**
 * An {@code AuthenticationHandler} encapsulates a possibly multi-step authentication protocol. Intended usage:
 *
 * <pre>
 * setParams(something);
 * start();
 * sendToken(getToken());
 * while (!isDone()) {
 *   setParams(receiveMessageAndExtractParams());
 *   process();
 *   Object t = getToken();
 *   if (t != null) {
 *     sendToken(t);
 *   }
 * }
 * </pre>
 *
 * An {@code AuthenticationHandler} may be stateful and therefore is a {@link Closeable}.
 *
 * @param <P> defining the parameter type for {@link #setParams(Object)}
 * @param <T> defining the token type for {@link #getToken()}
 */
public interface AuthenticationHandler<P, T>
    extends Closeable {

  /**
   * Produces the initial authentication token that can be then retrieved via {@link #getToken()}.
   *
   * @throws Exception if an error occurs
   */
  void start() throws Exception;

  /**
   * Produces the next authentication token, if any.
   *
   * @throws Exception if an error occurs
   */
  void process() throws Exception;

  /**
   * Sets the parameters for the next token generation via {@link #start()} or {@link #process()}.
   *
   * @param input to set, may be {@code null}
   */
  void setParams(P input);

  /**
   * Retrieves the last token generated.
   *
   * @return the token, or {@code null} if there is none
   * @throws Exception if an error occurs
   */
  T getToken() throws Exception;

  /**
   * Tells whether is authentication mechanism is done (successfully or unsuccessfully).
   *
   * @return whether this authentication is done
   */
  boolean isDone();

  @Override
  void close();

  void setDone(boolean done);
}
