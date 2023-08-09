/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.sftp.internal.util.ZonedDateTimeGreaterOrEqualAssessment;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class ZonedDateTimeGreaterOrEqualAssessmentTestCase {

  private static final ZonedDateTime LOWER_BOUND = ZonedDateTime.of(1983, 4, 20, 21, 15, 0, 0, ZoneId.systemDefault()); // .of(1983, 4, 20, 21, 15);
  private static final ZonedDateTime UPPER_BOUND = ZonedDateTime.of(2012, 3, 7, 18, 45, 0, 0, ZoneId.systemDefault()); // LocalDateTime.of(2012, 3, 7, 18, 45);

  private ZonedDateTimeGreaterOrEqualAssessment gteZonedDateTimeFunction = new ZonedDateTimeGreaterOrEqualAssessment();

  @Test
  public void isBefore() {
    assertThat(gteZonedDateTimeFunction.apply(LOWER_BOUND, UPPER_BOUND), is(true));
  }

  @Test
  public void isAfter() {
    assertThat(gteZonedDateTimeFunction.apply(UPPER_BOUND, LOWER_BOUND), is(false));
  }

  @Test
  public void isEquals() {
    assertThat(gteZonedDateTimeFunction.apply(UPPER_BOUND, UPPER_BOUND), is(true));
  }
}
