/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshException;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpClientTest {

  private SftpClient client = mock(SftpClient.class);

  @Test
  public void testSftpConnectionException() throws ConnectionException {
    doCallRealMethod().when(client).setProxyConfig(any());
    assertThrows(SftpConnectionException.class, () -> client.setProxyConfig(new SftpProxyConfig()));
  }

  @Test
  public void testSftpClientGetAttributesNull() throws ConnectionException, IOException {
    doCallRealMethod().when(client).getAttributes(any());
    assertNull(client.getAttributes(null));
  }

  @Test
  public void testSftpClientConfigureHostChecking() throws GeneralSecurityException, IOException {
    SftpClient client = new SftpClient("0.0.0.0", 8080, PRNGAlgorithm.SHA1PRNG, null);
    client.setKnownHostsFile("HostFile");
    assertThrows(SshException.class, () -> client.login("user"));
  }

  @Test
  public void testSftpClientCheckExists() throws GeneralSecurityException, IOException {
    doCallRealMethod().when(client).setIdentity(anyString(), anyString());
    assertThrows(IllegalArgumentException.class, () -> client.setIdentity("HostFile", "passphrase"));
  }

  @Test
  public void testSftpClientDisconnect() {
    SftpClient client = new SftpClient("host", 80, PRNGAlgorithm.SHA1PRNG, null);
    client.disconnect();
  }

  @Test
  public void testSftpClientGetFile() throws URISyntaxException {
    SftpClient client = new SftpClient("host", 80, PRNGAlgorithm.SHA1PRNG, null);
    URI uri = new URI("path");
    assertThrows(MuleRuntimeException.class, () -> client.getFile(uri));
  }

}
