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
  public void testCreateUriWithURISyntaxException() {
    String basePathWithColon = ":Illegal path with colon";
    assertThrows(IllegalPathException.class, () -> createUri(basePathWithColon, ""));
  }

  @Test
  public void testCreateUriWithNewLine() {
    String basePathWithNewLine = "test\npath";
    assertThrows(IllegalPathException.class, () -> createUri(basePathWithNewLine, ""));
  }

  @Test
  public void testCreateUriWithSeparator() {
    String basePathWithSeparator = "path/";
    assertEquals("path/path", createUri(basePathWithSeparator, "path").getPath());
  }

  @Test
  public void testCreateUriWithBasePathLengthOne() {
    String basePathWithSeparator = "p";
    assertEquals("ppath", createUri(basePathWithSeparator, "path").getPath());
  }

  @Test
  public void testCreateUriWithoutSeparator() {
    String basePathWithSeparator = "path";
    assertEquals("path/path", createUri(basePathWithSeparator, "path").getPath());
  }

  @Test
  public void testRegexPatternWithCaret() {
    assertThrows(PatternSyntaxException.class, () -> toRegexPattern("[^/"));
  }

  @Test
  public void testRegexPatternWithCaretAndAmpersand() {
    assertThrows(PatternSyntaxException.class, () -> toRegexPattern("[^&&"));
  }

  @Test
  public void testRegexPatternWithCaretAndDash() {
    assertThrows(PatternSyntaxException.class, () -> toRegexPattern("[^-"));
  }

  @Test
  public void testRegexPatternWithCaretAndBracket() {
    assertEquals("^[[^/]&&[\\^.]]$", toRegexPattern("[^.]"));
  }

}
