/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.extension.sftp.internal.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extra utilities to support usage of HTTP.
 */
public class HttpSupport {

  private final static Logger LOG = LoggerFactory
      .getLogger(HttpSupport.class);

  /** The {@code GET} HTTP method. */

  /**
   * Scan a RFC 7230 token as it appears in HTTP headers.
   *
   * @param header
   *            to scan in
   * @param from
   *            index in {@code header} to start scanning at
   * @return the index after the token, that is, on the first non-token
   *         character or {@code header.length}
   * @throws IndexOutOfBoundsException
   *             if {@code from < 0} or {@code from > header.length()}
   *
   * @see <a href="https://tools.ietf.org/html/rfc7230#appendix-B">RFC 7230,
   *      Appendix B: Collected Grammar; "token" production</a>
   * @since 5.10
   */
  public static int scanToken(String header, int from) {
    int length = header.length();
    int i = from;
    if (i < 0 || i > length) {
      throw new IndexOutOfBoundsException();
    }
    while (i < length) {
      char c = header.charAt(i);
      switch (c) {
        case '!':
        case '#':
        case '$':
        case '%':
        case '&':
        case '\'':
        case '*':
        case '+':
        case '-':
        case '.':
        case '^':
        case '_':
        case '`':
        case '|':
        case '~':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          i++;
          break;
        default:
          if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            i++;
            break;
          }
          return i;
      }
    }
    return i;
  }

  private HttpSupport() {
    // Utility class only.
  }
}
