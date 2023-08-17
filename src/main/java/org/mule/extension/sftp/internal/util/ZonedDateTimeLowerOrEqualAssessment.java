/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import java.time.ZonedDateTime;
import java.util.function.BiFunction;

/**
 * A Boolean {@link BiFunction} which returns {@code true} if a given {@link ZonedDateTime} is prior or equal to a reference date
 *
 * @since 1.0
 */
public final class ZonedDateTimeLowerOrEqualAssessment implements BiFunction<ZonedDateTime, ZonedDateTime, Boolean> {

  /**
   * @param criteria the reference value
   * @param value    the value to be tested
   * @return {@code true} if {@code value} is prior or equal to {@code criteria}
   */
  @Override
  public Boolean apply(ZonedDateTime criteria, ZonedDateTime value) {
    // DO NOT USE comparteTo
    return value.isBefore(criteria) || value.isEqual(criteria);
  }
}
