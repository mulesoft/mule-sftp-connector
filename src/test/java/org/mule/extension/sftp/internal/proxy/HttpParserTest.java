/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.junit.jupiter.api.BeforeAll;
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
    List<String> reply =
        Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth",
                      " lineWithWhitespace: text", "key:", "");
    assertEquals("lineWithWhitespace", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithEquals() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace=== , text");
    assertEquals("lineWithWhitespace===", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithComma() {
    List<String> reply =
        Arrays.asList("key:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace= ,text \"text\"");
    assertEquals("lineWithWhitespace=", HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithQuote() {
    List<String> reply =
        Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth!",
                      " lineWithWhitespace= \"text\"");
    assertNull(HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER).get(0).getToken());
  }

  @Test
  void testGetAuthenticationHeadersWithQuoteAndSlash() {
    List<String> reply =
        Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: auth!",
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

}
