package org.mule.extension.sftp.internal.connection;

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.proxy.HttpClientConnector;
import org.mule.extension.sftp.internal.proxy.Socks5ClientConnector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.DefaultConnectFuture;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.client.session.SessionFactory;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.future.CancelFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoConnectFuture;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.helpers.AbstractSession;

public class MuleSftpClient extends org.apache.sshd.client.SshClient {

  public SftpProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  public void setProxyConfig(SftpProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
  }

  private SftpProxyConfig proxyConfig;


  @Override
  protected SessionFactory createSessionFactory() {
    // Override the parent's default
    return new MuleSftpSessionFactory(this);
  }


  @Override
  protected ConnectFuture doConnect(
                                    String username, SocketAddress targetAddress,
                                    AttributeRepository context, SocketAddress localAddress,
                                    KeyIdentityProvider identities, HostConfigEntry hostConfig) {
    if (connector == null) {
      throw new IllegalStateException("SshClient not started. Please call start() method before connecting to a server");
    }

    InetSocketAddress originalAddress = new InetSocketAddress(hostConfig.getHost(), hostConfig.getPort());

    ConnectFuture connectFuture = new DefaultConnectFuture(username + "@" + targetAddress, null);
    SshFutureListener<IoConnectFuture> listener =
        createConnectCompletionListener(connectFuture, username, originalAddress, hostConfig);
    if (proxyConfig != null) {
      targetAddress = configureProxy(hostConfig);
    }

    IoConnectFuture connectingFuture = connector.connect(targetAddress, context, localAddress);
    connectFuture.addListener(c -> {
      if (c.isCanceled()) {
        connectingFuture.cancel();
      }
    });
    connectingFuture.addListener(listener);
    return connectFuture;
  }


  protected SshFutureListener<IoConnectFuture> createConnectCompletionListener(ConnectFuture connectFuture, String username,
                                                                               InetSocketAddress address,
                                                                               HostConfigEntry hostConfig) {
    return new SshFutureListener<IoConnectFuture>() {

      @Override
      @SuppressWarnings("synthetic-access")
      public void operationComplete(IoConnectFuture future) {
        if (future.isCanceled()) {
          CancelFuture cancellation = connectFuture.cancel();
          if (cancellation != null) {
            future.getCancellation().addListener(f -> cancellation.setCanceled(f.getBackTrace()));
          }
          return;
        }

        Throwable t = future.getException();
        if (t != null) {
          if (log.isDebugEnabled()) {
            log.debug("operationComplete({}@{}) failed ({}): {}",
                      username, address, t.getClass().getSimpleName(), t.getMessage());
          }
          connectFuture.setException(t);
        } else {
          IoSession ioSession = future.getSession();
          try {
            MuleSftpClientSession session = createSession(ioSession,
                                                          username, address, hostConfig);
            connectFuture.setSession(session);
          } catch (RuntimeException e) {
            connectFuture.setException(e);
            ioSession.close(true);
          }
        }
      }

      @Override
      public String toString() {
        return "ConnectCompletionListener[" + username + "@" + address + "]";
      }
    };
  }

  private InetSocketAddress configureProxy(HostConfigEntry hostConfig) {
    InetSocketAddress proxyAddress = new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort());
    InetSocketAddress remoteAddress = new InetSocketAddress(hostConfig.getHost(), hostConfig.getPort());

    if (proxyAddress.isUnresolved()) {
      proxyAddress = new InetSocketAddress(proxyAddress.getHostName(),
                                           proxyAddress.getPort());
    }

    switch (proxyConfig.getProtocol()) {
      case HTTP:
        setClientProxyConnector(new HttpClientConnector(proxyAddress, remoteAddress, proxyConfig.getUsername(),
                                                        proxyConfig.getPassword()));
        break;
      case SOCKS5:
        setClientProxyConnector(new Socks5ClientConnector(proxyAddress, remoteAddress, proxyConfig.getUsername(),
                                                          proxyConfig.getPassword()));
        break;
      default:
        // should never get here, except a new type was added to the enum and not handled
        throw new IllegalArgumentException(String.format("Proxy protocol %s not recognized", proxyConfig.getProtocol()));
    }
    return proxyAddress;
  }

  private MuleSftpClientSession createSession(IoSession ioSession,
                                              String username, InetSocketAddress address,
                                              HostConfigEntry hostConfig) {
    AbstractSession rawSession = AbstractSession.getSession(ioSession);
    if (!(rawSession instanceof MuleSftpClientSession)) {
      throw new IllegalStateException("Wrong session type: " //$NON-NLS-1$
          + rawSession.getClass().getCanonicalName());
    }
    MuleSftpClientSession session = (MuleSftpClientSession) rawSession;
    session.setUsername(username);
    session.setConnectAddress(address);

    return session;
  }

  private static class MuleSftpSessionFactory extends SessionFactory {

    public MuleSftpSessionFactory(MuleSftpClient client) {
      super(client);
    }

    @Override
    protected ClientSessionImpl doCreateSession(IoSession ioSession)
        throws Exception {
      return new MuleSftpClientSession(getClient(), ioSession);
    }
  }
}
