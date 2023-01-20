package org.mule.extension.sftp.internal.proxy.socks5;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.ietf.jgss.GSSContext;
import org.mule.extension.sftp.internal.auth.GssApiAuthentication;

import java.io.IOException;

import static java.text.MessageFormat.format;

/**
 * @see <a href="https://tools.ietf.org/html/rfc1961">RFC 1961</a>
 */
class SocksGssApiAuthentication
    extends GssApiAuthentication<Buffer, Buffer> {

  private static final byte SOCKS5_GSSAPI_VERSION = 1;

  private static final byte SOCKS5_GSSAPI_TOKEN = 1;

  private static final int SOCKS5_GSSAPI_FAILURE = 0xFF;

  private final Socks5ClientConnector socks5ClientConnector;

  public SocksGssApiAuthentication(Socks5ClientConnector socks5ClientConnector) {
    super(socks5ClientConnector.getProxyAddress());
    this.socks5ClientConnector = socks5ClientConnector;
  }

  @Override
  protected GSSContext createContext() throws Exception {
    return socks5ClientConnector.getContext();
  }

  @Override
  public Buffer getToken() throws Exception {
    if (token == null) {
      return null;
    }
    Buffer buffer = new ByteArrayBuffer(4 + token.length, false);
    buffer.putByte(SOCKS5_GSSAPI_VERSION);
    buffer.putByte(SOCKS5_GSSAPI_TOKEN);
    buffer.putByte((byte) ((token.length >> 8) & 0xFF));
    buffer.putByte((byte) (token.length & 0xFF));
    buffer.putRawBytes(token);
    return buffer;
  }

  @Override
  protected byte[] extractToken(Buffer input) throws Exception {
    if (socks5ClientConnector.getContext() == null) {
      return null;
    }
    int version = input.getUByte();
    if (version != SOCKS5_GSSAPI_VERSION) {
      throw new IOException("format(SshdText.get().proxySocksGssApiVersionMismatch, socks5ClientConnector.remoteAddress, Integer.toString(version))");
    }
    int msgType = input.getUByte();
    if (msgType == SOCKS5_GSSAPI_FAILURE) {
      throw new IOException("format(SshdText.get().proxySocksGssApiFailure, socks5ClientConnector.remoteAddress)");
    } else if (msgType != SOCKS5_GSSAPI_TOKEN) {
      throw new IOException("format( SshdText.get().proxySocksGssApiUnknownMessage, socks5ClientConnector.remoteAddress, Integer.toHexString(msgType & 0xFF))");
    }
    if (input.available() >= 2) {
      int length = (input.getUByte() << 8) + input.getUByte();
      if (input.available() >= length) {
        byte[] value = new byte[length];
        if (length > 0) {
          input.getRawBytes(value);
        }
        return value;
      }
    }
    throw new IOException("format(SshdText.get().proxySocksGssApiMessageTooShort, socks5ClientConnector.remoteAddress)");
  }
}
