/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.exception;

import org.junit.jupiter.api.Test;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

@SmallTest
public class PathConversionExceptionTest {

  private static final String TEST_MESSAGE = "Test path conversion error";
  private static final String CAUSE_MESSAGE = "Original URI syntax error";

  @Test
  void testConstructorWithMessageAndCause() {
    IOException cause = new IOException(CAUSE_MESSAGE);
    PathConversionException exception = new PathConversionException(TEST_MESSAGE, cause);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals(CAUSE_MESSAGE, exception.getCause().getMessage());
  }

  @Test
  void testConstructorWithNullMessageAndCause() {
    IOException cause = new IOException(CAUSE_MESSAGE);
    PathConversionException exception = new PathConversionException(null, cause);

    assertNull(exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void testConstructorWithMessageAndNullCause() {
    PathConversionException exception = new PathConversionException(TEST_MESSAGE, null);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithURISyntaxException() {
    URISyntaxException uriException = new URISyntaxException("invalid://uri with spaces", "Illegal character in authority");
    PathConversionException exception = new PathConversionException(uriException);

    assertEquals("Failed to convert URL to URI", exception.getMessage());
    assertEquals(uriException, exception.getCause());
    assertTrue(exception.getCause() instanceof URISyntaxException);
  }

  @Test
  void testConstructorWithNullURISyntaxException() {
    PathConversionException exception = new PathConversionException((URISyntaxException) null);

    assertEquals("Failed to convert URL to URI", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testExceptionInheritance() {
    PathConversionException exception = new PathConversionException(TEST_MESSAGE, new IOException());

    assertTrue(exception instanceof RuntimeException);
    assertTrue(exception instanceof Exception);
    assertTrue(exception instanceof Throwable);
  }

  @Test
  void testNestedCauseChain() {
    IOException originalCause = new IOException("Network error");
    URISyntaxException intermediateCause = new URISyntaxException("bad://uri", "Invalid scheme");
    intermediateCause.initCause(originalCause);
    PathConversionException exception = new PathConversionException(intermediateCause);

    assertEquals(intermediateCause, exception.getCause());
    assertEquals(originalCause, exception.getCause().getCause());
  }

  @Test
  void testToStringContainsMessage() {
    PathConversionException exception = new PathConversionException(TEST_MESSAGE, new IOException());
    String exceptionString = exception.toString();

    assertTrue(exceptionString.contains("PathConversionException"));
    assertTrue(exceptionString.contains(TEST_MESSAGE));
  }

  @Test
  void testGetStackTrace() {
    PathConversionException exception = new PathConversionException(TEST_MESSAGE, new IOException());
    StackTraceElement[] stackTrace = exception.getStackTrace();

    assertNotNull(stackTrace);
    assertTrue(stackTrace.length > 0);
    // Should contain reference to this test method
    assertTrue(java.util.Arrays.stream(stackTrace)
        .anyMatch(element -> element.getMethodName().equals("testGetStackTrace")));
  }

  @Test
  void testSpecificURISyntaxExceptionScenarios() {
    // Test with different types of URI syntax errors
    URISyntaxException malformedScheme = new URISyntaxException("ht tp://example.com", "Illegal character in scheme");
    PathConversionException exception1 = new PathConversionException(malformedScheme);
    assertEquals("Failed to convert URL to URI", exception1.getMessage());

    URISyntaxException illegalPath = new URISyntaxException("file:///path with spaces", "Illegal character in path");
    PathConversionException exception2 = new PathConversionException(illegalPath);
    assertEquals("Failed to convert URL to URI", exception2.getMessage());

    URISyntaxException illegalQuery = new URISyntaxException("http://example.com?param=val ue", "Illegal character in query");
    PathConversionException exception3 = new PathConversionException(illegalQuery);
    assertEquals("Failed to convert URL to URI", exception3.getMessage());
  }

  @Test
  void testWithDifferentCauseTypes() {
    // Test with various cause types
    IllegalArgumentException illegalArg = new IllegalArgumentException("Invalid argument");
    PathConversionException exception1 = new PathConversionException("Custom message", illegalArg);
    assertEquals("Custom message", exception1.getMessage());
    assertEquals(illegalArg, exception1.getCause());

    NumberFormatException numberFormat = new NumberFormatException("For input string: abc");
    PathConversionException exception2 = new PathConversionException("Number conversion failed", numberFormat);
    assertEquals("Number conversion failed", exception2.getMessage());
    assertEquals(numberFormat, exception2.getCause());
  }

  @Test
  void testRuntimeExceptionBehavior() {
    // Verify it behaves like a RuntimeException (unchecked)
    PathConversionException exception = new PathConversionException(TEST_MESSAGE, new IOException());

    // Should be able to throw without declaring in method signature
    assertThrows(PathConversionException.class, () -> {
      throwPathConversionException(exception);
    });
  }

  private void throwPathConversionException(PathConversionException exception) {
    // This method doesn't declare throws PathConversionException because it's a RuntimeException
    throw exception;
  }
}
