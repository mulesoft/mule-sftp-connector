/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.common.io.IoWriteFuture;
import org.mule.extension.sftp.internal.proxy.StatefulProxyConnector;

import org.apache.sshd.client.ClientFactoryManager;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.Readable;

import java.util.List;

@SuppressWarnings("java:S110")
public class MuleSftpClientSession extends ClientSessionImpl {

  private StatefulProxyConnector proxyHandler;

  public MuleSftpClientSession(ClientFactoryManager client, IoSession ioSession) throws Exception {
    super(client, ioSession);
  }

  public void setProxyHandler(StatefulProxyConnector handler) {
    proxyHandler = handler;
  }


  @Override
  protected byte[] sendKexInit() throws Exception {
    StatefulProxyConnector proxy = proxyHandler;
    if (proxy != null) {
      // We must not block here; the framework starts reading messages
      // from the peer only once the initial sendKexInit() has
      // returned!
      proxy.runWhenDone(() -> {
        MuleSftpClientSession.super.sendKexInit();
        return null;
      });
      // This is called only from the ClientSessionImpl
      // constructor, where the return value is ignored.
      return new byte[0];
    }
    return super.sendKexInit();
  }

  @Override
  public void messageReceived(Readable buffer) throws Exception {
    StatefulProxyConnector proxy = proxyHandler;
    if (proxy != null) {
      proxy.messageReceived(getIoSession(), buffer);
    } else {
      super.messageReceived(buffer);
    }
  }

  @Override
  protected IoWriteFuture sendIdentification(String ident,
                                             List<String> extraLines)
      throws Exception {
    StatefulProxyConnector proxy = proxyHandler;
    if (proxy != null) {
      // We must not block here; the framework starts reading messages
      // from the peer only once the initial sendKexInit() following
      // this call to sendIdentification() has returned!
      proxy.runWhenDone(() -> {
        MuleSftpClientSession.super.sendIdentification(ident, extraLines);
        return null;
      });
      // Called only from the ClientSessionImpl constructor, where the
      // return value is ignored.
      return null;
    }
    return super.sendIdentification(ident, extraLines);
  }


}
