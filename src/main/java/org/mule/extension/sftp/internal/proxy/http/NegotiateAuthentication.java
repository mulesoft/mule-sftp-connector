/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy.http;

import org.mule.extension.sftp.internal.auth.GssApiAuthentication;
import org.mule.extension.sftp.internal.proxy.AuthenticationChallenge;
import org.mule.extension.sftp.internal.proxy.GssApiMechanisms;
import org.mule.runtime.core.api.util.Base64;

import org.ietf.jgss.GSSContext;

/**
 * @see <a href="https://tools.ietf.org/html/rfc4559">RFC 4559</a>
 */
class NegotiateAuthentication
    extends GssApiAuthentication<AuthenticationChallenge, String>
    implements HttpAuthenticationHandler {

  private final HttpClientConnector httpClientConnector;

  public NegotiateAuthentication(HttpClientConnector httpClientConnector) {
    super(httpClientConnector.getProxyAddress());
    this.httpClientConnector = httpClientConnector;
  }

  @Override
  public String getName() {
    return "Negotiate"; //$NON-NLS-1$
  }

  @Override
  public String getToken() throws Exception {
    return getName() + ' ' + Base64.encodeBytes(token);
  }

  @Override
  protected GSSContext createContext() throws Exception {
    return GssApiMechanisms.createContext(GssApiMechanisms.SPNEGO,
                                          GssApiMechanisms.getCanonicalName(httpClientConnector.getProxyAddress()));
  }

  @Override
  protected byte[] extractToken(AuthenticationChallenge input)
      throws Exception {
    String received = input.getToken();
    if (received == null) {
      return new byte[0];
    }
    return Base64.decode(received);
  }

}
