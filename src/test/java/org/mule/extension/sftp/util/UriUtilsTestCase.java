/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.util;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.tck.size.SmallTest;

import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mule.extension.sftp.internal.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.util.UriUtils.toRegexPattern;

@SmallTest
public class UriUtilsTestCase {

  @Test
  void testCreateUriWithURISyntaxException() {
    String basePathWithColon = ":Illegal path with colon";
    assertThrows(IllegalPathException.class, () -> createUri(basePathWithColon, ""));
  }

  @Test
  void testCreateUriWithNewLine() {
    String basePathWithNewLine = "test\npath";
    assertThrows(IllegalPathException.class, () -> createUri(basePathWithNewLine, ""));
  }

  @Test
  void testCreateUriWithSeparator() {
    String basePathWithSeparator = "path/";
    assertEquals("path/path", createUri(basePathWithSeparator, "path").getPath());
  }

  @Test
  void testCreateUriWithBasePathLengthOne() {
    String basePathWithSeparator = "p";
    assertEquals("ppath", createUri(basePathWithSeparator, "path").getPath());
  }

  @Test
  void testCreateUriWithoutSeparator() {
    String basePathWithSeparator = "path";
    assertEquals("path/path", createUri(basePathWithSeparator, "path").getPath());
  }

  @Test
  void testRegexPatternWithCaretAndSlash() {
    String globPatternWithCaretAndSlash = "[^/";
    assertThrows(PatternSyntaxException.class, () -> toRegexPattern(globPatternWithCaretAndSlash));
  }

  @Test
  void testRegexPatternWithCaretAndAmpersand() {
    String globPatternWithCaretAndAmpersand = "[^&&";
    assertThrows(PatternSyntaxException.class, () -> toRegexPattern(globPatternWithCaretAndAmpersand));
  }

  @Test
  void testRegexPatternWithCaretAndDash() {
    String globPatternWithCaretAndDash = "[^-";
    assertThrows(PatternSyntaxException.class, () -> toRegexPattern(globPatternWithCaretAndDash));
  }

  @Test
  void testRegexPatternWithCaretAndBracket() {
    String globPatternWithCaretAndBracket = "[^.]";
    assertEquals("^[[^/]&&[\\^.]]$", toRegexPattern(globPatternWithCaretAndBracket));
  }

}
