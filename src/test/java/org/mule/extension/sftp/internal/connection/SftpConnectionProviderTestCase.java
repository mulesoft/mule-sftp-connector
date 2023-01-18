/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.write;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.sftp.SftpServer.PASSWORD;
import static org.mule.extension.sftp.SftpServer.USERNAME;
import static org.mule.extension.sftp.api.SftpAuthenticationMethod.GSSAPI_WITH_MIC;
import static org.mule.extension.sftp.random.alg.PRNGAlgorithm.AUTOSELECT;
import static org.mule.extension.sftp.random.alg.PRNGAlgorithm.NativePRNG;
import static org.mule.extension.sftp.random.alg.PRNGAlgorithm.NativePRNGBlocking;
import static org.mule.extension.sftp.random.alg.PRNGAlgorithm.NativePRNGNonBlocking;
import static org.mule.extension.sftp.random.alg.PRNGAlgorithm.SHA1PRNG;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;

import org.apache.ftpserver.command.impl.PORT;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.extension.sftp.internal.SftpConnector;
import org.mule.extension.sftp.random.alg.PRNGAlgorithm;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SftpConnectionProviderTestCase extends AbstractMuleTestCase {

  private static final String HOST = "localhost";
  private static final int TIMEOUT = 10;
  private static final String PASSPHRASE = "francis";

  private File hostFile;
  private File identityFile;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Mock
  private SftpConnector config;

  @Mock
  private SshClient sshClient;

  @Mock
  private org.apache.sshd.sftp.client.SftpClient sftp;
  @Mock
  private ClientSession session;

  private SftpConnectionProvider provider = new SftpConnectionProvider();


  @Before
  public void before() throws Exception {
    hostFile = new File(folder.getRoot(), "host");
    identityFile = new File(folder.getRoot(), "identity");

    write(hostFile, "hostFile");
    write(identityFile, "jason bourne");
    provider.setHost(HOST);
    provider.setPassword(PASSWORD);
    provider.setUsername(USERNAME);
    provider.setConnectionTimeout(10);
    provider.setConnectionTimeoutUnit(SECONDS);
    provider.setPrngAlgorithm(SHA1PRNG);
    provider.setPreferredAuthenticationMethods(unmodifiableSet(new HashSet<>(singletonList(GSSAPI_WITH_MIC))));
    provider.setKnownHostsFile(hostFile.getAbsolutePath());

    provider.setClientFactory(new SftpClientFactory() {

      @Override
      public SftpClient createInstance(String host, int port) {
        return new SftpClient(host, port);
      }
    });

    ConnectFuture connectFuture = Mockito.mock(ConnectFuture.class);

    //when(channel.pwd()).thenReturn("/");
    when(sshClient.connect(USERNAME, HOST, 22)).thenReturn(connectFuture);
    when(connectFuture.verify(new Long(SECONDS.toMillis(TIMEOUT)))).thenReturn(connectFuture);
    when(connectFuture.getSession()).thenReturn(session);
    //when(session.openChannel("sftp")).thenReturn(channel);
  }

  @Test
  public void identityFileWithPassPhrase() throws Exception {
    provider.setIdentityFile(identityFile.getAbsolutePath());
    provider.setPassphrase(PASSPHRASE);

    login();

    verify(session).addPublicKeyIdentity(any());
  }

//  @Test
//  public void whenSHA1PRNGlgorithmIsSetSetThenLogginPropertiesAreSet() throws Exception {
//    assertPropertyCorrectWith(SHA1PRNG);
//  }
//
//  @Test
//  public void whenAUTOSELECTlgorithmIsSetSetThenLogginPropertiesAreSet() throws Exception {
//    assertPropertyCorrectWith(AUTOSELECT);
//  }
//
//  @Test
//  public void whenNativePRNGBlockinglgorithmIsSetSetThenLogginPropertiesAreSet() throws Exception {
//    assertPropertyCorrectWith(NativePRNGBlocking);
//  }
//
//  @Test
//  public void whenNativePRNGlgorithmIsSetSetThenLogginPropertiesAreSet() throws Exception {
//    assertPropertyCorrectWith(NativePRNG);
//  }
//
//  @Test
//  public void whenNativePRNGNonBlockinglgorithmIsSetSetThenLogginPropertiesAreSet() throws Exception {
//    assertPropertyCorrectWith(NativePRNGNonBlocking);
//  }
//
//  @Test
//  public void identityFileWithoutPassPhrase() throws Exception {
//    provider.setIdentityFile(identityFile.getAbsolutePath());
//
//    login();
//
//    assertSimpleIdentity();
//  }
//
//  private void assertPropertyCorrectWith(PRNGAlgorithm algorithm) throws Exception {
//    provider.setPrngAlgorithm(algorithm);
//    provider.connect();
//    Properties properties = captureLoginProperties();
//    assertThat(properties.get("random"), equalTo(algorithm.getImplementationClassName()));
//  }
//
//  private void assertSimpleIdentity() throws JSchException {
//    verify(jsch).addIdentity(identityFile.getAbsolutePath());
//  }
//
//  @Test
//  public void simpleCredentials() throws Exception {
//    provider.setPassword(PASSWORD);
//    login();
//
//    assertPassword();
//  }
//
//  @Test
//  public void simpleCredentialsPlusIdentity() throws Exception {
//    provider.setIdentityFile(identityFile.getAbsolutePath());
//    provider.setPassword(PASSWORD);
//
//    login();
//
//    assertPassword();
//    assertSimpleIdentity();
//  }
//
//  @Test
//  public void noKnownHosts() throws Exception {
//    provider.setKnownHostsFile(null);
//    provider.connect();
//
//    Properties properties = captureLoginProperties();
//    //assertThat(properties.getProperty(STRICT_HOST_KEY_CHECKING), equalTo("no"));
//  }
//
//  private void assertPassword() {
//    verify(session).addPasswordIdentity(PASSWORD);
//  }

  private void login() throws Exception {
    SftpFileSystem fileSystem = provider.connect();
    SftpClient client = spy(fileSystem.getClient());
    assertThat(fileSystem.getBasePath(), is(""));
    verify(sshClient).connect(USERNAME, HOST, 22).verify(new Long(SECONDS.toMillis(TIMEOUT)).intValue());
    verify(sshClient).setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);
//    verify(session).connect();
//    verify(channel).connect();

    Properties properties = captureLoginProperties();
    //    assertThat(properties.getProperty(PREFERRED_AUTHENTICATION_METHODS), equalTo(GSSAPI_WITH_MIC.toString()));
    //    assertThat(properties.getProperty(STRICT_HOST_KEY_CHECKING), equalTo("ask"));
    verify(client, never()).changeWorkingDirectory(anyString());
  }

  private Properties captureLoginProperties() {
    ArgumentCaptor<Properties> propertiesCaptor = forClass(Properties.class);
    //verify(session).setConfig(propertiesCaptor.capture());

    return propertiesCaptor.getValue();
  }
}
