package org.mule.extension.sftp.internal.proxy.http;

import org.mule.extension.sftp.internal.auth.AuthenticationHandler;
import org.mule.extension.sftp.internal.proxy.AuthenticationChallenge;

interface HttpAuthenticationHandler extends AuthenticationHandler<AuthenticationChallenge, String> {

  public String getName();
}
