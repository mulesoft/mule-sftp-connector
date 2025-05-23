/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.sftp.internal.proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A basic parser for HTTP response headers. Handles status lines and
 * authentication headers (WWW-Authenticate, Proxy-Authenticate).
 *
 * @see <a href="https://tools.ietf.org/html/rfc7230">RFC 7230</a>
 * @see <a href="https://tools.ietf.org/html/rfc7235">RFC 7235</a>
 */
public final class HttpParser {

  /**
   * An exception indicating some problem parsing HTPP headers.
   */
  public static class ParseException extends Exception {

    private static final long serialVersionUID = -1634090143702048640L;

    /**
     * Creates a new {@link HttpParser.ParseException} without cause.
     */
    public ParseException() {
      super();
    }

    /**
     * Creates a new {@link HttpParser.ParseException} with the given {@code cause}.
     *
     * @param cause
     *            {@link Throwable} that caused this exception, or
     *            {@code null} if none
     */
    public ParseException(Throwable cause) {
      super(cause);
    }
  }

  private HttpParser() {
    // No instantiation
  }

  /**
   * Parse a HTTP response status line.
   *
   * @param line
   *            to parse
   * @return the {@link StatusLine}
   * @throws HttpParser.ParseException
   *             if the line cannot be parsed or has the wrong HTTP version
   */
  public static StatusLine parseStatusLine(String line)
      throws ParseException {
    // Format is HTTP/<version> Code Reason
    int firstBlank = line.indexOf(' ');
    if (firstBlank < 0) {
      throw new ParseException();
    }
    int secondBlank = line.indexOf(' ', firstBlank + 1);
    if (secondBlank < 0) {
      // Accept the line even if the (according to RFC 2616 mandatory)
      // reason is missing.
      secondBlank = line.length();
    }
    int resultCode;
    try {
      resultCode = Integer.parseUnsignedInt(
                                            line.substring(firstBlank + 1, secondBlank));
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
    // Again, accept even if the reason is missing
    String reason = ""; //$NON-NLS-1$
    if (secondBlank < line.length()) {
      reason = line.substring(secondBlank + 1);
    }
    return new StatusLine(line.substring(0, firstBlank), resultCode,
                          reason);
  }

  /**
   * Extract the authentication headers from the header lines. It is assumed
   * that the first element in {@code reply} is the raw status line as
   * received from the server. It is skipped. Line processing stops on the
   * first empty line thereafter.
   *
   * @param reply
   *            The complete (header) lines of the HTTP response
   * @param authenticationHeader
   *            to look for (including the terminating ':'!)
   * @return a list of {@link AuthenticationChallenge}s found.
   */
  public static List<AuthenticationChallenge> getAuthenticationHeaders(
                                                                       List<String> reply, String authenticationHeader) {
    List<AuthenticationChallenge> challenges = new ArrayList<>();
    Iterator<String> lines = reply.iterator();
    // We know we have at least one line. Skip the response line.
    lines.next();
    StringBuilder value = null;
    while (lines.hasNext()) {
      String line = lines.next();
      if (line.isEmpty()) {
        break;
      }
      if (Character.isWhitespace(line.charAt(0))) {
        // Continuation line.
        if (value == null) {
          // Skip if we have no current value
          continue;
        }
        // Skip leading whitespace
        int i = skipWhiteSpace(line, 1);
        value.append(' ').append(line, i, line.length());
        continue;
      }
      if (value != null) {
        parseChallenges(challenges, value.toString());
        value = null;
      }
      int firstColon = line.indexOf(':');
      if (firstColon > 0 && authenticationHeader
          .equalsIgnoreCase(line.substring(0, firstColon + 1))) {
        value = new StringBuilder(line.substring(firstColon + 1));
      }
    }
    if (value != null) {
      parseChallenges(challenges, value.toString());
    }
    return challenges;
  }

  private static void parseChallenges(
                                      List<AuthenticationChallenge> challenges,
                                      String header) {
    // Comma-separated list of challenges, each itself a scheme name
    // followed optionally by either: a comma-separated list of key=value
    // pairs, where the value may be a quoted string with backslash escapes,
    // or a single token value, which itself may end in zero or more '='
    // characters. Ugh.
    int length = header.length();
    for (int i = 0; i < length;) {
      int start = skipWhiteSpace(header, i);
      int end = HttpSupport.scanToken(header, start);
      if (end <= start) {
        break;
      }
      AuthenticationChallenge challenge = new AuthenticationChallenge(
                                                                      header.substring(start, end));
      challenges.add(challenge);
      i = parseChallenge(challenge, header, end);
    }
  }

  private static int parseChallenge(AuthenticationChallenge challenge,
                                    String header, int from) {
    int length = header.length();
    boolean first = true;
    for (int start = from; start <= length; first = false) {
      start = skipWhiteSpace(header, start);
      int end = HttpSupport.scanToken(header, start);
      if (end == start) {
        return handleEmptyToken(header, start);
      }

      int next = skipWhiteSpace(header, end);
      if (shouldReturnTokenOnly(header, length, next)) {
        return handleTokenOnly(challenge, header, start, end, next, first);
      }

      int nextStart = skipWhiteSpace(header, next + 1);
      if (nextStart >= length) {
        return handleKeyWithoutValue(challenge, header, start, end, next);
      }

      if (checkHandleTokenWithMultipleEquals(nextStart,end,header)) {
        return handleTokenWithMultipleEquals(challenge, header, start, end, nextStart, length);
      }

      if (header.charAt(nextStart) == ',') {
        return handleKeyFollowedByComma(challenge, header, start, end, next, nextStart);
      } else {
        start = handleKeyValuePair(challenge, header, start, end, nextStart);
      }

      start = skipWhiteSpace(header, start);
      if (start < length && header.charAt(start) == ',') {
        start++;
      }
    }
    return length;
  }

  private static boolean checkHandleTokenWithMultipleEquals(int nextStart,int end, String header){
      return nextStart == end + 1 && header.charAt(nextStart) == '=';
  }

  private static int handleEmptyToken(String header, int start) {
    if (start < header.length() && header.charAt(start) == ',') {
      return start + 1;
    }
    return start;
  }

  private static boolean shouldReturnTokenOnly(String header, int length, int next) {
    return next >= length || header.charAt(next) != '=';
  }

  private static int handleTokenOnly(AuthenticationChallenge challenge, String header,
                                     int start, int end, int next, boolean first) {
    if (first) {
      challenge.setToken(header.substring(start, end));
      if (next < header.length() && header.charAt(next) == ',') {
        next++;
      }
      return next;
    }
    return start;
  }

  private static int handleKeyWithoutValue(AuthenticationChallenge challenge, String header,
                                           int start, int end, int next) {
    if (next == end) {
      challenge.setToken(header.substring(start, end + 1));
    } else {
      challenge.addArgument(header.substring(start, end), null);
    }
    return skipWhiteSpace(header, next + 1);
  }

  private static int handleTokenWithMultipleEquals(AuthenticationChallenge challenge,
                                                   String header, int start, int end,
                                                   int nextStart, int length) {
    end = nextStart + 1;
    while (end < length && header.charAt(end) == '=') {
      end++;
    }
    challenge.setToken(header.substring(start, end));
    end = skipWhiteSpace(header, end);
    if (end < length && header.charAt(end) == ',') {
      end++;
    }
    return end;
  }

  private static int handleKeyFollowedByComma(AuthenticationChallenge challenge,
                                              String header, int start, int end,
                                              int next, int nextStart) {
    if (next == end) {
      challenge.setToken(header.substring(start, end + 1));
      return nextStart + 1;
    }
    challenge.addArgument(header.substring(start, end), null);
    return nextStart + 1;
  }

  private static int handleKeyValuePair(AuthenticationChallenge challenge, String header,
                                        int start, int end, int nextStart) {
    if (header.charAt(nextStart) == '"') {
      int[] nextEnd = {nextStart + 1};
      String value = scanQuotedString(header, nextStart + 1, nextEnd);
      challenge.addArgument(header.substring(start, end), value);
      return nextEnd[0];
    } else {
      int nextEnd = HttpSupport.scanToken(header, nextStart);
      challenge.addArgument(header.substring(start, end), header.substring(nextStart, nextEnd));
      return nextEnd;
    }
  }

  private static int skipWhiteSpace(String header, int i) {
    int length = header.length();
    while (i < length && Character.isWhitespace(header.charAt(i))) {
      i++;
    }
    return i;
  }

  private static String scanQuotedString(String header, int from, int[] to) {
    StringBuilder result = new StringBuilder();
    int length = header.length();
    boolean quoted = false;
    int i = from;
    while (i < length) {
      char c = header.charAt(i++);
      if (quoted) {
        result.append(c);
        quoted = false;
      } else if (c == '\\') {
        quoted = true;
      } else if (c == '"') {
        break;
      } else {
        result.append(c);
      }
    }
    to[0] = i;
    return result.toString();
  }
}
