/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.file.common.api.util;

import org.junit.Test;
import org.mule.extension.sftp.internal.util.ZonedDateTimeGreaterOrEqualAssessment;
import org.mule.extension.sftp.internal.util.ZonedDateTimeLowerOrEqualAssessment;

import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ZonedDateTimeTimezoneDifferencesAssessmentTestCase {

  private static final ZonedDateTime DATE_TIME_A = ZonedDateTime.parse("1983-04-20T15:35:00.000-02:00"); // ZonedDateTime.of(1983, 4, 20, 15, 15, 0, 0, ZoneId.of("Etc/GMT-2"));
  private static final ZonedDateTime DATE_TIME_B = ZonedDateTime.parse("1983-04-20T16:35:00.000-01:00"); // ZonedDateTime.of(1983, 4, 20, 16, 15, 0, 0, ZoneId.of("Etc/GMT-1"));
  private static final ZonedDateTime DATE_TIME_C = ZonedDateTime.parse("1983-04-20T17:35:00.000Z"); // ZonedDateTime.of(1983, 4, 20, 17, 15, 0, 0, ZoneId.of("Etc/GMT0"));

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
