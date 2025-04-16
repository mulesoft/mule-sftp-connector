/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.random.alg.PRNGAlgorithm;
import org.mule.extension.sftp.internal.connection.SftpConnectionSettings;
import org.mule.tck.size.SmallTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SmallTest
public class SftpConnectionSettingsTestCase {

  private SftpConnectionSettings settings;
  private SftpConnectionSettings settings2;

  @BeforeEach
  public void setup() {
    settings = new SftpConnectionSettings();
    settings2 = new SftpConnectionSettings();

    settings.setHost("localhost");
    settings.setPort(22);
    settings.setUsername("user");
    settings.setPassword("pass");
    settings.setPassphrase("phrase");
    settings.setIdentityFile("/path/to/identity");
    settings.setPrngAlgorithm(PRNGAlgorithm.SHA1PRNG);
    settings.setKexHeader(true);

    settings2.setHost("localhost");
    settings2.setPort(22);
    settings2.setUsername("user");
    settings2.setPassword("pass");
    settings2.setPassphrase("phrase");
    settings2.setIdentityFile("/path/to/identity");
    settings2.setPrngAlgorithm(PRNGAlgorithm.SHA1PRNG);
    settings2.setKexHeader(true);
  }

  @Test
  public void testSftpConnectionSettingsEquals() {
    assertEquals(settings, settings2);
  }

  @Test
  public void testSftpConnectionSettingsSelfEquals() {
    assertEquals(settings, settings);
  }

  @Test
  public void testSftpConnectionSettingsEqualsWithNull() {
    assertNotEquals(settings, null);
  }

  @Test
  public void testSftpConnectionSettingsNotEquals() {
    SftpConnectionSettings otherObj = new SftpConnectionSettings();
    assertNotEquals(settings, otherObj);
  }

  @Test
  public void testTimeoutSettingsHashCode() {
    assertEquals(settings.hashCode(), settings2.hashCode());
  }
}
