/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.matcher.FileMatcher;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * A set of criterias used to filter files stored in a FTP server. The file's properties are to be represented on an instance of
 * {@link SftpFileAttributes}.
 *
 * @since 1.0
 */
@Alias("matcher")
@TypeDsl(allowTopLevelDefinition = true)
public class SftpFileMatcher extends FileMatcher<SftpFileMatcher, SftpFileAttributes> {

  private static final Logger LOGGER = getLogger(SftpFileMatcher.class);
  private AtomicBoolean alreadyLoggedWarning = new AtomicBoolean();

  /**
   * Files created before this date are rejected.
   */
  @Parameter
  @Summary("Files created before this date are rejected.")
  @Example("2015-06-03T13:21:58+00:00")
  @Optional
  private ZonedDateTime timestampSince;

  /**
   * Files created after this date are rejected.
   */
  @Parameter
  @Summary("Files created after this date are rejected.")
  @Example("2015-06-03T13:21:58+00:00")
  @Optional
  private ZonedDateTime timestampUntil;

  /**
   * Minimum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with
   * {@link #timeUnit}.
   */
  @Parameter
  @Summary("Minimum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with timeUnit.")
  @Example("10000")
  @Optional
  private Long notUpdatedInTheLast;

  /**
   * Maximum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with
   * {@link #timeUnit}.
   */
  @Parameter
  @Summary("Maximum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with timeUnit.")
  @Example("10000")
  @Optional
  private Long updatedInTheLast;

  /**
   * A {@link TimeUnit} which qualifies the {@link #updatedInTheLast} and the {@link #notUpdatedInTheLast} attributes.
   * <p>
   * Defaults to {@code MILLISECONDS}
   */
  @Parameter
  @Summary("Time unit to be used to interpret the parameters 'notUpdatedInTheLast' and 'updatedInTheLast'")
  @Optional(defaultValue = "MILLISECONDS")
  private TimeUnit timeUnit;

  /**
   * Enables you to configure an external file system matcher as case sensitive or insensitive.
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean caseSensitive;


  public SftpFileMatcher() {
    super();
  }

  @Override
  protected Predicate<SftpFileAttributes> addConditions(Predicate<SftpFileAttributes> predicate) {
    setPredicateType(PredicateType.EXTERNAL_FILE_SYSTEM);
    setCaseSensitive(caseSensitive);

    if (timestampSince != null) {
      predicate = predicate.and(attributes -> attributes.getTimestamp() == null
          || fileTimeSince.apply(timestampSince, attributes.getTimestamp()));
    }

    if (timestampUntil != null) {
      predicate = predicate.and(attributes -> attributes.getTimestamp() == null
          || fileTimeUntil.apply(timestampUntil, attributes.getTimestamp()));
    }

    // We want to make sure that the same time is used when comparing multiple files consecutively.
    ZonedDateTime now = ZonedDateTime.now();

    if (notUpdatedInTheLast != null) {
      predicate = predicate.and(attributes -> {
        checkTimestampPrecision(attributes);
        return attributes.getTimestamp() == null
            || fileTimeUntil.apply(minusTime(now, notUpdatedInTheLast, timeUnit), attributes.getTimestamp());
      });
    }

    if (updatedInTheLast != null) {
      predicate = predicate.and(attributes -> {
        checkTimestampPrecision(attributes);
        return attributes.getTimestamp() == null
            || fileTimeSince.apply(minusTime(now, updatedInTheLast, timeUnit), attributes.getTimestamp());
      });
    }

    return predicate;
  }

  private void checkTimestampPrecision(SftpFileAttributes attributes) {
    if (LOGGER.isWarnEnabled() && alreadyLoggedWarning.compareAndSet(false, true) && isSecondsOrLower(timeUnit)
        && (attributes.getTimestamp()==null || attributes.getTimestamp().getSecond() == 0 && attributes.getTimestamp().getNano() == 0)) {
      LOGGER
          .warn(format("The required timestamp precision %s cannot be met. The server may not support it.",
                       timeUnit));
    }
  }

  private boolean isSecondsOrLower(TimeUnit timeUnit) {
    return timeUnit == TimeUnit.SECONDS || timeUnit == TimeUnit.MILLISECONDS || timeUnit == TimeUnit.MICROSECONDS
        || timeUnit == TimeUnit.NANOSECONDS;
  }

  private ZonedDateTime minusTime(ZonedDateTime zonedDateTime, Long time, TimeUnit timeUnit) {
    return zonedDateTime.minus(getTimeInMillis(time, timeUnit), ChronoUnit.MILLIS);
  }

  private long getTimeInMillis(Long time, TimeUnit timeUnit) {
    return timeUnit.toMillis(time);
  }

  public SftpFileMatcher setTimestampSince(ZonedDateTime timestampSince) {
    this.timestampSince = timestampSince;
    return this;
  }

  public SftpFileMatcher setTimestampUntil(ZonedDateTime timestampUntil) {
    this.timestampUntil = timestampUntil;
    return this;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public void setUpdatedInTheLast(Long updatedInTheLast) {
    this.updatedInTheLast = updatedInTheLast;
  }

  public void setNotUpdatedInTheLast(Long notUpdatedInTheLast) {
    this.notUpdatedInTheLast = notUpdatedInTheLast;
  }

  public ZonedDateTime getTimestampSince() {
    return timestampSince;
  }

  public void setTimestampsince(ZonedDateTime timestampSince) {
    this.timestampSince = timestampSince;
  }


  public ZonedDateTime getTimestampUntil() {
    return timestampUntil;
  }

  public void setTimestampuntil(ZonedDateTime timestampUntil) {
    this.timestampUntil = timestampUntil;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public Long getUpdatedInTheLast() {
    return updatedInTheLast;
  }

  public Long getNotUpdatedInTheLast() {
    return notUpdatedInTheLast;
  }

  public boolean isCaseSensitive() {
    return this.caseSensitive;
  }

  public void setCasesensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

}
