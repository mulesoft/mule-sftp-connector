/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.junit.Test;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@SmallTest
public class HttpParserTest {

  private static final String AUTHENTICATOR_HEADER = "Proxy-Authentication:";

  @Test
  public void testGetAuthenticationHeadersWithWhitespace() {
    List<String> reply =
        Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth",
                      " lineWithWhitespace: text", "key:", "");
    assertEquals("lineWithWhitespace", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithEquals() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace=== , text");
    assertEquals("lineWithWhitespace===", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithComma() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace= ,text \"text\"");
    assertEquals("lineWithWhitespace=", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithQuote() {
    List<String> reply =
        Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace= \"text\"");
    assertNull(HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithQuoteAndSlash() {
    List<String> reply =
        Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace= \"\\text\"");
    assertNull(HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  public void testParseStatusLineFirstBlankException() {
    try {
      HttpParser.parseStatusLine("NoBlank");
      fail("Expected ParseException");
    } catch (HttpParser.ParseException e) {
      // Expected
    }
  }

  @Test
  public void testParseStatusLineSecondBlankException() {
    try {
      HttpParser.parseStatusLine("Single Blank");
      fail("Expected ParseException");
    } catch (HttpParser.ParseException e) {
      // Expected
    }
  }

  @Test
  public void testGetAuthenticationHeadersWithKeyValuePair() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " key=value");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertNull(challenge.getToken());
    assertEquals("value", challenge.getArguments().get("key"));
  }

  @Test
  public void testGetAuthenticationHeadersWithEmptyToken() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " ,");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertNull(challenge.getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithTokenOnly() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " token");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("token", challenge.getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithKeyWithoutValue() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " key=");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("key=", challenge.getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithKeyFollowedByComma() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " key=,value");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("key=", challenge.getToken());
  }

  @Test
  public void testGetAuthenticationHeadersWithMultipleEquals() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " token===value");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("token===", challenge.getToken());
  }
}
