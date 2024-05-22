/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.kex.KeyExchangeFactory;
import org.apache.sshd.server.ServerBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class SftpClientWithExternalConfigTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testConnectionWithEnvironmentVars() throws Exception {
    SftpTestHarness sftpTestHarness = new SftpTestHarness();
    List<KeyExchangeFactory> keyExchangeFactoryList =
        NamedFactory.setUpTransformedFactories(true, Collections.singletonList(BuiltinDHFactories.dhg14), ServerBuilder.DH2KEX);
    try {
      Properties properties = new Properties();
      properties.put("KexAlgorithms", "diffie-hellman-group14-sha1");
      sftpTestHarness.setupClientAndServer(keyExchangeFactoryList, () -> properties);
    } finally {
      sftpTestHarness.doAfter();
    }
  }

  @Test
  public void testConnectionWithoutEnvironmentVars() throws Exception {
    expectedException.expect(SshException.class);
    expectedException.expectMessage("Unable to negotiate key exchange for kex algorithms");
    SftpTestHarness sftpTestHarness = new SftpTestHarness();
    List<KeyExchangeFactory> keyExchangeFactoryList =
        NamedFactory.setUpTransformedFactories(true, Collections.singletonList(BuiltinDHFactories.dhg14), ServerBuilder.DH2KEX);
    try {
      sftpTestHarness.setupClientAndServer(keyExchangeFactoryList, Properties::new);
    } finally {
      sftpTestHarness.doAfter();
    }
  }

}
