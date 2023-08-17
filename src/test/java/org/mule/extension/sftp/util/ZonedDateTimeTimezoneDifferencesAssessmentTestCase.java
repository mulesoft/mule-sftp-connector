/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.util;

import org.junit.Test;
import org.mule.extension.sftp.internal.util.ZonedDateTimeGreaterOrEqualAssessment;
import org.mule.extension.sftp.internal.util.ZonedDateTimeLowerOrEqualAssessment;

import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ZonedDateTimeTimezoneDifferencesAssessmentTestCase {

  private static final ZonedDateTime DATE_TIME_A = ZonedDateTime.parse("1983-04-20T15:35:00.000-02:00");
  private static final ZonedDateTime DATE_TIME_B = ZonedDateTime.parse("1983-04-20T16:35:00.000-01:00");
  private static final ZonedDateTime DATE_TIME_C = ZonedDateTime.parse("1983-04-20T17:35:00.000Z");

  private ZonedDateTimeLowerOrEqualAssessment lteZonedDateTimeFunction = new ZonedDateTimeLowerOrEqualAssessment();
  private ZonedDateTimeGreaterOrEqualAssessment gteZonedDateTimeFunction = new ZonedDateTimeGreaterOrEqualAssessment();

  @Test
  public void testThatDatesAreEqual() {
    assertThat(lteZonedDateTimeFunction.apply(DATE_TIME_A, DATE_TIME_B), is(true));
    assertThat(gteZonedDateTimeFunction.apply(DATE_TIME_A, DATE_TIME_B), is(true));

    assertThat(lteZonedDateTimeFunction.apply(DATE_TIME_B, DATE_TIME_C), is(true));
    assertThat(gteZonedDateTimeFunction.apply(DATE_TIME_B, DATE_TIME_C), is(true));

    assertThat(lteZonedDateTimeFunction.apply(DATE_TIME_C, DATE_TIME_A), is(true));
    assertThat(gteZonedDateTimeFunction.apply(DATE_TIME_C, DATE_TIME_A), is(true));

  }

}
