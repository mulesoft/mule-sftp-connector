package org.mule.extension.sftp.internal.connection;

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.proxy.http.HttpClientConnector;
import org.mule.extension.sftp.internal.proxy.socks5.Socks5ClientConnector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.DefaultConnectFuture;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoConnectFuture;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;

public class ConnectorSftpClient extends org.apache.sshd.client.SshClient {

  public SftpProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  public void setProxyConfig(SftpProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
  }

  private SftpProxyConfig proxyConfig;


  @Override
  protected ConnectFuture doConnect(
                                    String username, SocketAddress targetAddress,
                                    AttributeRepository context, SocketAddress localAddress,
                                    KeyIdentityProvider identities, HostConfigEntry hostConfig) {
    if (connector == null) {
      throw new IllegalStateException("SshClient not started. Please call start() method before connecting to a server");
    }

    ConnectFuture connectFuture = new DefaultConnectFuture(username + "@" + targetAddress, null);
    SshFutureListener<IoConnectFuture> listener = createConnectCompletionListener(
                                                                                  connectFuture, username, targetAddress,
                                                                                  identities, hostConfig);
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
  

  private InetSocketAddress configureProxy(HostConfigEntry hostConfig) {
    InetSocketAddress proxyAddress = new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort());
    InetSocketAddress remoteAddress = new InetSocketAddress(hostConfig.getHost(), hostConfig.getPort());
    switch (proxyConfig.getProtocol()) {
      case HTTP:
        setClientProxyConnector(proxyConfig.getUsername() != null && proxyConfig.getPassword() != null
            ? new HttpClientConnector(proxyAddress, remoteAddress, proxyConfig.getUsername(),
                                      proxyConfig.getPassword().toCharArray())
            : new HttpClientConnector(proxyAddress, remoteAddress));
        break;
      case SOCKS5:
        setClientProxyConnector(proxyConfig.getUsername() != null && proxyConfig.getPassword() != null
            ? new Socks5ClientConnector(proxyAddress, remoteAddress, proxyConfig.getUsername(),
                                        proxyConfig.getPassword().toCharArray())
            : new Socks5ClientConnector(proxyAddress, remoteAddress));
        break;
      default:
        // should never get here, except a new type was added to the enum and not handled
        throw new IllegalArgumentException(String.format("Proxy protocol %s not recognized", proxyConfig.getProtocol()));
    }
    return proxyAddress;
  }
}
