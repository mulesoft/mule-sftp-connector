/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.mule.extension.sftp.AllureConstants.SftpFeature.SFTP_EXTENSION;
import static org.mule.extension.sftp.api.exceptions.FileError.CANNOT_REACH;
import static org.mule.extension.sftp.api.exceptions.FileError.CONNECTION_TIMEOUT;
import static org.mule.extension.sftp.api.exceptions.FileError.INVALID_CREDENTIALS;
import static org.mule.extension.sftp.api.exceptions.FileError.UNKNOWN_HOST;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import org.mule.extension.sftp.api.SftpConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

import java.util.Arrays;
import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(SFTP_EXTENSION)
@Story("Negative Connectivity Testing")
public class SftpNegativeConnectivityTestCase extends CommonSftpConnectorTestCase {

  private static final Matcher<Exception> ANYTHING =
      is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(SftpConnectionException.class))));
  private final String name;
  private TestConnectivityUtils utils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  public SftpNegativeConnectivityTestCase(String name, SftpTestHarness testHarness, String ftpConfigFile) {
    super(name, testHarness, ftpConfigFile);
    this.name = name;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"sftp", new org.mule.extension.sftp.SftpTestHarness(), SFTP_CONNECTION_XML}});
  }

  @Override
  protected String getConfigFile() {
    return name + "-negative-connectivity-test.xml";
  }

  @Before
  public void setUp() {
    utils = new TestConnectivityUtils(registry);
  }

  @Ignore
  @Test
  public void configInvalidCredentials() {
    utils.assertFailedConnection(name + "ConfigInvalidCredentials", ANYTHING, is(errorType(INVALID_CREDENTIALS)));
  }

  @Ignore
  @Test
  public void configConnectionTimeout() {
    utils.assertFailedConnection(name + "ConfigConnectionTimeout", ANYTHING, is(errorType(CONNECTION_TIMEOUT)));
  }

  @Ignore
  @Test
  public void connectionRefused() {
    utils.assertFailedConnection(name + "ConfigConnectionRefused", ANYTHING, is(errorType(CANNOT_REACH)));
  }

  @Ignore
  @Test
  public void configMissingCredentials() {
    utils.assertFailedConnection(name + "ConfigMissingCredentials", ANYTHING, is(errorType(INVALID_CREDENTIALS)));
  }

  @Ignore
  @Test
  public void configUnknownHost() {
    utils.assertFailedConnection(name + "ConfigUnknownHost", ANYTHING, is(errorType(UNKNOWN_HOST)));
  }

}
