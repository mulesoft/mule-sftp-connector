/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.connection.TimeoutSettings;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TimeoutSettingsTestCase {

  private TimeoutSettings timeout;
  private TimeoutSettings timeout2;

  @BeforeEach
  void setup() {
    timeout = new TimeoutSettings();
    timeout2 = new TimeoutSettings();

    timeout.setResponseTimeoutUnit(TimeUnit.NANOSECONDS);
    timeout.setResponseTimeout(5000);
    timeout.setConnectionTimeoutUnit(TimeUnit.NANOSECONDS);
    timeout.setConnectionTimeout(5000);

    timeout2.setResponseTimeoutUnit(TimeUnit.NANOSECONDS);
    timeout2.setResponseTimeout(5000);
    timeout2.setConnectionTimeoutUnit(TimeUnit.NANOSECONDS);
    timeout2.setConnectionTimeout(5000);
  }

  @Test
  void testTimeoutSettingsEquals() {
    assertEquals(timeout, timeout2);
  }

  @Test
  void testTimeoutSettingsSelfEquals() {
    assertEquals(timeout, timeout);
  }

  @Test
  void testTimeoutSettingsEqualsWithNull() {
    assertNotEquals(null, timeout);
  }

  @Test
  void testTimeoutSettingsNotEquals() {
    TimeoutSettings otherObj = new TimeoutSettings();
    assertNotEquals(timeout, otherObj);
  }

  @Test
  void testTimeoutSettingsHashCode() {
    assertEquals(timeout.hashCode(), timeout2.hashCode());
  }
}
