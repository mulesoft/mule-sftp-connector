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

import static org.junit.jupiter.api.Assertions.*;

@SmallTest
public class ProxyConnectionExceptionTest {

  private static final String TEST_MESSAGE = "Test proxy connection error";
  private static final String CAUSE_MESSAGE = "Original cause error";

  @Test
  void testConstructorWithMessage() {
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithNullMessage() {
    ProxyConnectionException exception = new ProxyConnectionException((String) null);

    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    IOException cause = new IOException(CAUSE_MESSAGE);
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE, cause);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertEquals(cause, exception.getCause());
    assertEquals(CAUSE_MESSAGE, exception.getCause().getMessage());
  }

  @Test
  void testConstructorWithNullMessageAndCause() {
    IOException cause = new IOException(CAUSE_MESSAGE);
    ProxyConnectionException exception = new ProxyConnectionException(null, cause);

    assertNull(exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void testConstructorWithMessageAndNullCause() {
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE, null);

    assertEquals(TEST_MESSAGE, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithCause() {
    IOException cause = new IOException(CAUSE_MESSAGE);
    ProxyConnectionException exception = new ProxyConnectionException(cause);

    // When constructed with just a cause, the message is typically the toString() of the cause
    assertTrue(exception.getMessage().contains(CAUSE_MESSAGE));
    assertEquals(cause, exception.getCause());
  }

  @Test
  void testConstructorWithNullCause() {
    ProxyConnectionException exception = new ProxyConnectionException((Throwable) null);

    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testExceptionInheritance() {
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE);

    assertTrue(exception instanceof Exception);
    assertTrue(exception instanceof Throwable);
  }

  @Test
  void testNestedCauseChain() {
    IOException originalCause = new IOException("Network error");
    RuntimeException intermediateCause = new RuntimeException("Intermediate error", originalCause);
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE, intermediateCause);

    assertEquals(intermediateCause, exception.getCause());
    assertEquals(originalCause, exception.getCause().getCause());
  }

  @Test
  void testToStringContainsMessage() {
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE);
    String exceptionString = exception.toString();

    assertTrue(exceptionString.contains("ProxyConnectionException"));
    assertTrue(exceptionString.contains(TEST_MESSAGE));
  }

  @Test
  void testGetStackTrace() {
    ProxyConnectionException exception = new ProxyConnectionException(TEST_MESSAGE);
    StackTraceElement[] stackTrace = exception.getStackTrace();

    assertNotNull(stackTrace);
    assertTrue(stackTrace.length > 0);
    // Should contain reference to this test method
    assertTrue(java.util.Arrays.stream(stackTrace)
        .anyMatch(element -> element.getMethodName().equals("testGetStackTrace")));
  }
}
