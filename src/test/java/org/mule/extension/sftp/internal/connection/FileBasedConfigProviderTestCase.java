/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class FileBasedConfigProviderTestCase {

  @Test
  public void testFetchConfigFileProperties() {
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider("mule_sshd_config");
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(1, properties.size());
    assertEquals("diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha512@ssh.com,diffie-hellman-group1-sha1",
                 properties.getProperty("KexAlgorithms"));
  }

  @Test
  public void testFileNotFound() {
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider("mule_sshd");
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(0, properties.size());
  }

  @Test
  public void testWithConfigFileEmpty() {
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider("");
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(0, properties.size());
  }

}
