package org.mule.extension.sftp.internal.proxy.socks5;

/**
 * Authentication methods for SOCKS5.
 *
 * @see <a href= "https://www.iana.org/assignments/socks-methods/socks-methods.xhtml">SOCKS Methods, IANA.org</a>
 */
public enum SocksAuthenticationMethod {

  ANONYMOUS(0), GSSAPI(1), PASSWORD(2),
  // CHALLENGE_HANDSHAKE(3),
  // CHALLENGE_RESPONSE(5),
  // SSL(6),
  // NDS(7),
  // MULTI_AUTH(8),
  // JSON(9),
  NONE_ACCEPTABLE(0xFF);

  private final byte value;

  SocksAuthenticationMethod(int value) {
    this.value = (byte) value;
  }

  public byte getValue() {
    return value;
  }
}
