/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.matcher;

import static org.mule.extension.sftp.api.PredicateType.LOCAL_FILE_SYSTEM;
import static org.mule.extension.sftp.api.matcher.MatchPolicy.INCLUDE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.lang.String.format;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.PredicateType;
import org.mule.extension.sftp.internal.util.ZonedDateTimeGreaterOrEqualAssessment;
import org.mule.extension.sftp.internal.util.ZonedDateTimeLowerOrEqualAssessment;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link FileAttributes} instance is compliant with a number of criterias. This
 * builder is stateful and not thread-safe. A new instance should be use per each desired {@link Predicate}.
 * <p>
 * This builder can either be used programmatically or through Mule's SDK since its internal state is annotated with the
 * {@link Parameter} annotation.
 * <p>
 * Criterias are evaluated using an {@code AND} operator, meaning that for the predicate to accept a file, ALL the criterias must
 * be complied with.
 * <p>
 * None of the criteria fields are mandatory. If a particular criteria is not specified, then it's simply not applied on the
 * evaluation.
 * <p>
 * The class is also given the &quot;matcher&quot; alias to make it DSL/XML friendly.
 *
 * @param <T> {@code this} instance concrete type. It allows to extend this class while allowing setter chains
 * @param <A> The concrete implementation of {@link FileAttributes} that this builder uses to assert the file properties
 * @since 1.0
 */
public abstract class FileMatcher<T extends FileMatcher, A extends FileAttributes> {

  private static final String SIZE_MUST_BE_GREATER_THAN_ZERO_MESSAGE =
      "Matcher attribute '%s' must be greater than zero but '%d' was received";
  public static final boolean DEFAULT_CASE_SENSITIVE = true;
  protected final ZonedDateTimeLowerOrEqualAssessment FILE_TIME_UNTIL = new ZonedDateTimeLowerOrEqualAssessment();
  protected final ZonedDateTimeGreaterOrEqualAssessment FILE_TIME_SINCE = new ZonedDateTimeGreaterOrEqualAssessment();

  /**
   * A matching pattern to be applied on the file name. This pattern needs to be consistent with the rules of
   * {@link org.mule.extension.sftp.api.matcher.PathMatcherPredicate}
   */
  @Parameter
  @Optional
  @Summary("A matching pattern to be applied on the file name.")
  private String filenamePattern;

  /**
   * A matching pattern to be applied on the file path. This pattern needs to be consistent with the rules of
   * {@link org.mule.extension.sftp.api.matcher.PathMatcherPredicate}
   */
  @Parameter
  @Optional
  @Summary("A matching pattern to be applied on the file path")
  private String pathPattern;

  /**
   * If {@code true}, the predicate will only accept files which are directories. If {@code false}, the predicate will only accept
   * files which are not directories. If not set, then the criteria doesn't apply.
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  @Summary("Indicates whether accept only directories or non directories files")
  private org.mule.extension.sftp.api.matcher.MatchPolicy directories = INCLUDE;

  /**
   * If {@code true}, the predicate will only accept files which are not directories nor symbolic links. If {@code false}, the
   * predicate will only accept files which are directories or symbolic links. If not set, then the criteria doesn't apply.
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  @Summary("Indicates whether accept only regular files (files which are not directories, nor symbolic links) "
      + "or only not regular files")
  private org.mule.extension.sftp.api.matcher.MatchPolicy regularFiles = INCLUDE;

  /**
   * If {@code true}, the predicate will only accept files which are symbolic links. If {@code false}, the predicate will only
   * accept files which are symbolic links. If not set, then the criteria doesn't apply.
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  @Summary("Indicates whether accept only symbolic links files or accept only not symbolic links files")
  private org.mule.extension.sftp.api.matcher.MatchPolicy symLinks = INCLUDE;

  /**
   * The minimum file size in bytes. Files smaller than this are rejected
   */
  @Parameter
  @Optional
  private Long minSize;

  /**
   * The maximum file size in bytes. Files larger than this are rejected
   */
  @Parameter
  @Optional
  private Long maxSize;


  private PredicateType predicateType = LOCAL_FILE_SYSTEM;

  private boolean caseSensitive = DEFAULT_CASE_SENSITIVE;

  /**
   * Builds a {@link Predicate} from the criterias in {@code this} builder's state.
   *
   * @return a {@link Predicate}
   */
  public Predicate<A> build() {
    Predicate<A> predicate = payload -> true;
    if (filenamePattern != null) {
      org.mule.extension.sftp.api.matcher.PathMatcherPredicate pathMatcher =
          new org.mule.extension.sftp.api.matcher.PathMatcherPredicate(filenamePattern, predicateType, caseSensitive);
      predicate = predicate.and(payload -> pathMatcher.test(payload.getName()));
    }

    if (pathPattern != null) {
      org.mule.extension.sftp.api.matcher.PathMatcherPredicate pathMatcher =
          new PathMatcherPredicate(pathPattern, predicateType, caseSensitive);
      predicate = predicate.and(payload -> pathMatcher.test(payload.getPath()));
    }

    if (!directories.acceptsAll()) {
      predicate = predicate.and(attributes -> directories.asBoolean().get().equals(attributes.isDirectory()));
    }

    if (!regularFiles.acceptsAll()) {
      predicate = predicate.and(attributes -> regularFiles.asBoolean().get().equals(attributes.isRegularFile()));
    }

    if (!symLinks.acceptsAll()) {
      predicate = predicate.and(attributes -> symLinks.asBoolean().get().equals(attributes.isSymbolicLink()));
    }

    if (minSize != null) {
      checkArgument(minSize > 0, format(SIZE_MUST_BE_GREATER_THAN_ZERO_MESSAGE, "minSize", minSize));
      predicate = predicate.and(attributes -> attributes.getSize() >= minSize);
    }

    if (maxSize != null) {
      checkArgument(maxSize > 0, format(SIZE_MUST_BE_GREATER_THAN_ZERO_MESSAGE, "maxSize", maxSize));
      predicate = predicate.and(attributes -> attributes.getSize() <= maxSize);
    }

    return addConditions(predicate);
  }

  /**
   * This method is invoked by {@link #build()} before returning the built {@link Predicate}.
   * <p>
   * It allows extending classes to modify the returned {@link Predicate}.
   *
   * @param predicate the {@link Predicate} that is about to be returned by {@link #build()}
   * @return a new instance or the same one in case no modification is required.
   */
  protected Predicate<A> addConditions(Predicate<A> predicate) {
    return predicate;
  }

  protected ZonedDateTimeLowerOrEqualAssessment getFileTimeUntil() {
    return FILE_TIME_UNTIL;
  }


  protected ZonedDateTimeGreaterOrEqualAssessment getFileTimeSince() {
    return FILE_TIME_SINCE;
  }

  public String getFilenamePattern() {
    return filenamePattern;
  }

  public void setFileNamePattern(String filenamePattern) {
    this.filenamePattern = filenamePattern;
  }

  public String getPathPattern() {
    return pathPattern;
  }

  public void setPathpattern(String pathPattern) {
    this.pathPattern = pathPattern;
  }

  public org.mule.extension.sftp.api.matcher.MatchPolicy getDirectories() {
    return directories;
  }

  public void setdirectories(org.mule.extension.sftp.api.matcher.MatchPolicy directories) {
    this.directories = directories;
  }

  public org.mule.extension.sftp.api.matcher.MatchPolicy getRegularFiles() {
    return regularFiles;
  }

  public void setRegularfiles(org.mule.extension.sftp.api.matcher.MatchPolicy regularFiles) {
    this.regularFiles = regularFiles;
  }

  public org.mule.extension.sftp.api.matcher.MatchPolicy getSymLinks() {
    return symLinks;
  }

  public void setsymLinks(org.mule.extension.sftp.api.matcher.MatchPolicy symLinks) {
    this.symLinks = symLinks;
  }

  public Long getMinSize() {
    return minSize;
  }

  public void setminSize(Long minSize) {
    this.minSize = minSize;
  }

  public Long getMaxSize() {
    return maxSize;
  }

  public void setmaxSize(Long maxSize) {
    this.maxSize = maxSize;
  }

  public PredicateType getPredicateType() {
    return predicateType;
  }

  public void setpredicateType(PredicateType predicateType) {
    this.predicateType = predicateType;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setcaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public T setFilenamePattern(String filenamePattern) {
    this.filenamePattern = filenamePattern;
    return (T) this;
  }

  public T setPathPattern(String pathPattern) {
    this.pathPattern = pathPattern;
    return (T) this;
  }

  public T setDirectories(org.mule.extension.sftp.api.matcher.MatchPolicy directories) {
    this.directories = directories;
    return (T) this;
  }

  public T setRegularFiles(org.mule.extension.sftp.api.matcher.MatchPolicy regularFiles) {
    this.regularFiles = regularFiles;
    return (T) this;
  }

  public T setSymLinks(MatchPolicy symLinks) {
    this.symLinks = symLinks;
    return (T) this;
  }

  public T setMinSize(Long minSize) {
    this.minSize = minSize;
    return (T) this;
  }

  public T setMaxSize(Long maxSize) {
    this.maxSize = maxSize;
    return (T) this;
  }

  /**
   * @param predicateType {@link PredicateType} set the type of predicate
   * @return {@link FileMatcher}
   */
  public T setPredicateType(PredicateType predicateType) {
    this.predicateType = predicateType;
    return (T) this;
  }

  /**
   * @param caseSensitive set case sensitivity
   * @return {@link FileMatcher}
   */
  public T setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return (T) this;
  }
}
