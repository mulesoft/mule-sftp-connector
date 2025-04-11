/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.util;

import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.extension.sftp.internal.exception.SftpConnectionException;
import org.mule.tck.size.SmallTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

@SmallTest
public class UriUtilsTestCase {

  private String basePathWithColon = ":Illegal path with colon";
  private String basePathWithNewLine = "test\npath";
  private String filePath = "";

  @Test
  public void testCreateUriWithURISyntaxException() {
    assertThrows(IllegalPathException.class, () -> createUri(basePathWithColon, filePath));
  }

  @Test
  public void testCreateUriWithNewLine() {
    assertThrows(IllegalPathException.class, () -> createUri(basePathWithNewLine, filePath));
  }

}
