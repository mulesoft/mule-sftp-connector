/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

import static java.lang.String.format;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.tck.size.SmallTest;

import javax.inject.Inject;
import java.net.URI;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SftpClientTestCase {

  private static final String FILE_PATH = "/bla/file.txt";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private URI uri = createUri(FILE_PATH);

  @Mock
  private SshClient sshClient;

  @Mock
  private org.apache.sshd.sftp.client.SftpClient sftp;
  @Mock
  private ClientSession session;

  @Inject
  protected SchedulerService schedulerService;

  @InjectMocks
  private SftpClient client = new SftpClient(EMPTY, 0, PRNGAlgorithm.SHA1PRNG, schedulerService, null);

  public SftpClientTestCase() throws ConnectionException {}

  @Test
  public void returnNullOnUnexistingFile() throws Exception {
    when(sftp.stat(anyString())).thenThrow(new SftpException(SftpConstants.SSH_FX_NO_SUCH_FILE, "No such file"));
    assertThat(client.getAttributes(uri), is(nullValue()));
  }

  @Test
  public void exceptionIsThrownOnError() throws Exception {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(format("Could not obtain attributes for path %s", FILE_PATH));
    when(sftp.stat(anyString())).thenThrow(new SftpException(SftpConstants.SSH_FX_PERMISSION_DENIED, EMPTY));
    client.getAttributes(uri);
  }

  @Test
  public void expectConnectionExceptionWhenIOExceptionIsThrown() throws Exception {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(instanceOf(ConnectionException.class));
    when(sftp.stat(anyString())).thenThrow(new SftpException(SftpConstants.SSH_FX_CONNECTION_LOST, EMPTY));
    client.getAttributes(uri);
  }
}
