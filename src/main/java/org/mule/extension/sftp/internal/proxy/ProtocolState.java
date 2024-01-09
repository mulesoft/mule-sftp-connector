/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import static java.lang.String.format;

import org.mule.extension.sftp.internal.proxy.socks5.Socks5ClientConnector;
import org.mule.extension.sftp.internal.proxy.socks5.SocksAuthenticationMethod;

import java.io.IOException;

import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.BufferUtils;

public enum ProtocolState {

  NONE,

  INIT {

    @Override
    public void handleMessage(Socks5ClientConnector connector,
                              IoSession session, Buffer data)
        throws Exception {
      connector.versionCheck(data.getByte());
      SocksAuthenticationMethod authMethod = connector.getAuthMethod(
                                                                     data.getByte());
      switch (authMethod) {
        case ANONYMOUS:
          connector.sendConnectInfo(session);
          break;
        case PASSWORD:
          connector.doPasswordAuth(session);
          break;
        case GSSAPI:
          connector.doGssApiAuth(session);
          break;
        default:
          throw new IOException(format("Cannot authenticate to proxy %s %s",
                                       connector.proxyAddress.getAddress(),
                                       connector.proxyAddress.getPort()));
      }
    }
  },

  AUTHENTICATING {

    @Override
    public void handleMessage(Socks5ClientConnector connector,
                              IoSession session, Buffer data)
        throws Exception {
      connector.authStep(session, data);
    }
  },

  CONNECTING {

    @Override
    public void handleMessage(Socks5ClientConnector connector,
                              IoSession session, Buffer data)
        throws Exception {
      // Special case: when GSS-API authentication completes, the
      // client moves into CONNECTING as soon as the GSS context is
      // established and sends the connect request. This is per RFC
      // 1961. But for the server, RFC 1961 says it _should_ send an
      // empty token even if none generated when its server side
      // context is established. That means we may actually get an
      // empty token here. That message is 4 bytes long (and has
      // content 0x01, 0x01, 0x00, 0x00). We simply skip this message
      // if we get it here. If the server for whatever reason sends
      // back a "GSS failed" message (it shouldn't, at this point)
      // it will be two bytes 0x01 0xFF, which will fail the version
      // check.
      if (data.available() != 4) {
        connector.versionCheck(data.getByte());
        connector.establishConnection(data);
      }
    }
  },

  CONNECTED,

  FAILED;

  public void handleMessage(Socks5ClientConnector connector,
                            @SuppressWarnings("unused") IoSession session, Buffer data)
      throws Exception {
    throw new IOException(format("Unexpected message received from SOCKS5 proxy %s; client state %s: %s", connector.proxyAddress,
                                 this, BufferUtils.toHex(data.array())));
  }
}
