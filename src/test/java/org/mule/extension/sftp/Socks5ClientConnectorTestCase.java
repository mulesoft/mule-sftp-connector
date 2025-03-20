/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.junit.Test;
import org.mule.extension.sftp.internal.proxy.HttpParser;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

@SmallTest
public class HttpParserTestCase {

  private static final String AUTHENTICATOR_HEADER = "Proxy-Authentication:";
  private static final List<String> reply =
      Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: someContent",
                    " lineWithWhitespace: some", "key:", "");
  private static final List<String> reply2 =
      Arrays.asList("key:", "Proxy-Authentication: someContent!",
                    " lineWithWhitespace=== , someContent");
  private static final List<String> reply3 =
      Arrays.asList("key:", "Proxy-Authentication: someContent!",
                    " lineWithWhitespace= ,text \"content\"");
  private static final List<String> reply4 =
      Arrays.asList("key: ", " lineWithWhitespace:", "Proxy-Authentication: someContent!",
                    " lineWithWhitespace= \"someContent\"");

  @Test
  public void testGetAuthenticationHeaders() {
    HttpParser.getAuthenticationHeaders(reply, AUTHENTICATOR_HEADER);
    HttpParser.getAuthenticationHeaders(reply2, AUTHENTICATOR_HEADER);
    HttpParser.getAuthenticationHeaders(reply3, AUTHENTICATOR_HEADER);
    HttpParser.getAuthenticationHeaders(reply4, AUTHENTICATOR_HEADER);
  }

}
