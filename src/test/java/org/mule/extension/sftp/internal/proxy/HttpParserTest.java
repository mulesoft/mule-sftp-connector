/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.junit.jupiter.api.Test;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SmallTest
public class HttpParserTest {

  private static final String AUTHENTICATOR_HEADER = "Proxy-Authentication:";

  @Test
  void testGetAuthenticationHeadersWithWhitespace() {
    List<String> reply = Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth",
                                       " lineWithWhitespace: text", "key:", "");
    assertEquals("lineWithWhitespace", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithEquals() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!",
                                       " lineWithWhitespace=== , text");
    assertEquals("lineWithWhitespace===", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithComma() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!",
                                       " lineWithWhitespace= ,text \"text\"");
    assertEquals("lineWithWhitespace=", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithQuote() {
    List<String> reply = Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth!",
                                       " lineWithWhitespace= \"text\"");
    assertNull(HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithQuoteAndSlash() {
    List<String> reply = Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth!",
                                       " lineWithWhitespace= \"\\text\"");
    assertNull(HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testParseStatusLineFirstBlankException() {
    assertThrows(HttpParser.ParseException.class, () -> HttpParser.parseStatusLine("NoBlank"));
  }

  @Test
  void testParseStatusLineSecondBlankException() {
    assertThrows(HttpParser.ParseException.class, () -> HttpParser.parseStatusLine("Single Blank"));
  }

  @Test
  void testGetAuthenticationHeadersWithKeyValuePair() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " key=value");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertNull(challenge.getToken());
    assertEquals("value", challenge.getArguments().get("key"));
  }

  @Test
  void testGetAuthenticationHeadersWithEmptyToken() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " ,");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertNull(challenge.getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithTokenOnly() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " token");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("token", challenge.getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithKeyWithoutValue() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " key=");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("key=", challenge.getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithKeyFollowedByComma() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " key=,value");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("key=", challenge.getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithMultipleEquals() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " token===value");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertEquals("token===", challenge.getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithMultipleKeyValuePairs() {
    List<String> reply = Arrays.asList("key:", "Proxy-Authentication: auth!", " key1=value1,key2=value2");
    AuthenticationChallenge challenge = HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0);
    assertNull(challenge.getToken());
    assertEquals("value1", challenge.getArguments().get("key1"));
    assertEquals("value2", challenge.getArguments().get("key2"));
  }
}
