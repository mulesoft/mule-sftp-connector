/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FileBasedConfigProviderEmptyPropertiesTestCase {

  private String filePath;

  public FileBasedConfigProviderEmptyPropertiesTestCase(String filePath) {
    this.filePath = filePath;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"mule_sshd"}, {""}, {"src/test/resources"}});
  }

  @Test
  public void testForEmptyProperties() {
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider(filePath);
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(0, properties.size());
  }
}
