/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.junit.Test;
import org.mule.extension.sftp.api.SftpProxyConfig;
import org.mule.extension.sftp.internal.connection.provider.SftpConnectionProvider;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

@SmallTest
public class SftpConnectionProviderTestCase {

  private SftpConnectionProvider provider = new SftpConnectionProvider();
  private SftpConnectionProvider provider2 = new SftpConnectionProvider();


  @Test
  public void testSftpConnectionProvider() {
    provider.setProxyConfig(new SftpProxyConfig());
    provider.setConnectionTimeout(500);
    provider.setConnectionTimeoutUnit(TimeUnit.MICROSECONDS);
    provider.setResponseTimeout(500);
    provider.setResponseTimeoutUnit(TimeUnit.NANOSECONDS);



    //    settings2.setHost("localhost");
    //    settings2.setPort(22);
    //    settings2.setUsername("user");
    //    settings2.setPassword("pass");
    //    settings2.setPassphrase("phrase");
    //    settings2.setIdentityFile("/path/to/identity");
    //    settings2.setPrngAlgorithm(PRNGAlgorithm.SHA1PRNG);
    //    settings2.setKexHeader(true);
    //
    //    assertEquals(settings, settings2);
    //
    //    Object other = new Object();
    //    assertNotEquals(settings, other);
    //    settings.equals(settings);
    //
    //    settings.hashCode();
  }
}
