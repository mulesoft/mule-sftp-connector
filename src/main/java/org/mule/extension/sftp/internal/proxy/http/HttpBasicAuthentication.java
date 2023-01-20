package org.mule.extension.sftp.internal.proxy.http;

import org.mule.extension.sftp.internal.auth.BasicAuthentication;
import org.mule.extension.sftp.internal.proxy.AuthenticationChallenge;
import org.mule.runtime.core.api.util.Base64;

import java.io.IOException;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
 */
class HttpBasicAuthentication extends BasicAuthentication<AuthenticationChallenge, String> implements HttpAuthenticationHandler {

  private final HttpClientConnector httpClientConnector;
  private boolean asked;

  public HttpBasicAuthentication(HttpClientConnector httpClientConnector) {
    super(httpClientConnector.getProxyAddress(), httpClientConnector.getProxyUser(), httpClientConnector.getProxyPassword());
    this.httpClientConnector = httpClientConnector;
  }

  @Override
  public String getName() {
    return "Basic"; //$NON-NLS-1$
  }

  @Override
  protected void askCredentials() throws Exception {
    // We ask only once.
    if (asked) {
      throw new IllegalStateException(
                                      "Basic auth: already asked user for password"); //$NON-NLS-1$
    }
    asked = true;
    super.askCredentials();
    httpClientConnector.setDone(true);
  }

  @Override
  public String getToken() throws Exception {
    if (user.indexOf(':') >= 0) {
      throw new IOException("format(SshdText.get().proxyHttpInvalidUserName, proxy, user)");
    }
    byte[] rawUser = user.getBytes(UTF_8);
    byte[] toEncode = new byte[rawUser.length + 1 + password.length];
    System.arraycopy(rawUser, 0, toEncode, 0, rawUser.length);
    toEncode[rawUser.length] = ':';
    System.arraycopy(password, 0, toEncode, rawUser.length + 1,
                     password.length);
    Arrays.fill(password, (byte) 0);
    String result = Base64.encodeBytes(toEncode);
    Arrays.fill(toEncode, (byte) 0);
    return getName() + ' ' + result;
  }

}
