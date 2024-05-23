/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mule.extension.sftp.internal.connection.FileBasedConfigProvider.CONFIG_FILE_PATH_PROPERTY;

public class FileBasedConfigProviderTestCase {

  @Test
  public void testFetchConfigFileProperties() {
    System.setProperty(CONFIG_FILE_PATH_PROPERTY, "mule_sshd_config");
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider();
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(1, properties.size());
    assertEquals("diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha512@ssh.com,diffie-hellman-group1-sha1",
                 properties.getProperty("KexAlgorithms"));
  }

  @Test
  public void testFileNotFound() {
    System.setProperty(CONFIG_FILE_PATH_PROPERTY, "mule_sshd");
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider();
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(0, properties.size());
  }

}
