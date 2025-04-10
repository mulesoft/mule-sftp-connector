/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.Before;
import org.junit.Test;
import org.mule.extension.sftp.internal.connection.TimeoutSettings;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SmallTest
public class TimeoutSettingsTestCase {

  private TimeoutSettings timeout = new TimeoutSettings();
  private TimeoutSettings timeout2 = new TimeoutSettings();
  private final Integer connectionTimeout = 5000;
  private final Integer responseTimeout = 500;
  private final TimeUnit timeUnit = TimeUnit.NANOSECONDS;

  @Before
  public void setup() {
    timeout.setResponseTimeoutUnit(timeUnit);
    timeout.setResponseTimeout(responseTimeout);
    timeout.setConnectionTimeoutUnit(timeUnit);
    timeout.setConnectionTimeout(connectionTimeout);

    timeout2.setResponseTimeoutUnit(timeUnit);
    timeout2.setResponseTimeout(responseTimeout);
    timeout2.setConnectionTimeoutUnit(timeUnit);
    timeout2.setConnectionTimeout(connectionTimeout);
  }

  @Test
  public void testTimeoutSettingsEquals() {
    assertEquals(timeout, timeout2);

    Object other = new Object();
    assertNotEquals(timeout, other);
    timeout.equals(timeout);
  }

  @Test
  public void testTimeoutSettingsHashCode() {
    timeout.hashCode();
  }
}
