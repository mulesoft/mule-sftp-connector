/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.PatternSyntaxException;

/**
 * Class for creating and handling URIs.
 *
 * @since 1.3.0
 */
public final class UriUtils {

  private UriUtils() {}

  private static final String SEPARATOR = "/";
  private static final char EOL = 0;
  private static final String REGEX_META_CHARS = ".^$+{[]|()";
  private static final String GLOB_META_CHARS = "\\*?[{";

  /**
   * Creates an {@link URI} for a given path.
   *
   * @param path the path to the file or directory.
   * @return a {@link URI} representing the path in the following format (using the unix path separator):
   *         "/directory/subdirectory"
   */
  public static URI createUri(String path) {
    return createUri(path, "");
  }

  /**
   * Creates an {@link URI} for a given basePath and a filePath, resolving them in the process. This means that if the filePath is
   * absolute, then a uri representing it is returned. Otherwise, the basePath and filePath are combined together.
   *
   * @param basePath the path to the base directory.
   * @param filePath the path to the file.
   * @return a {@link URI} representing the resolved Path between the two given arguments.
   */
  public static URI createUri(String basePath, String filePath) {
    String fullPath;
    try {
      if (filePath.length() > 0) {
        if (Boolean.TRUE.equals(isAbsolute(filePath))) {
          fullPath = filePath;
        } else {
          fullPath = addSeparator(basePath) + filePath;
        }
      } else {
        fullPath = removeSeparator(basePath);
      }

      if (fullPath.split("\\r?\\n").length != 1) {
        throw new IllegalPathException("Path contains newline character: " + fullPath);
      }

      return new URI(null, null, fullPath, null);
    } catch (URISyntaxException e) {
      throw new IllegalPathException("Cannot convert given path into a valid Uri", e);
    }
  }

  /**
   * Adds a separator at the end of the given path. If the path already ends with the separator, then this method does nothing.
   */
  private static String addSeparator(String path) {
    return (path.endsWith(SEPARATOR) || path.length() == 1) ? path : path + SEPARATOR;
  }

  /**
   * Removes the separator at the end of the given path. If the path does not end with the separator, then this method does
   * nothing.
   */
  private static String removeSeparator(String path) {
    return (!path.endsWith(SEPARATOR) || path.length() == 1) ? path : path.substring(0, path.length() - 1);
  }

  /**
   * @param path the path to check
   * @return a {@link Boolean} indicating if the path is absolute or not.
   */
  private static Boolean isAbsolute(String path) {
    return (path.length() > 0 && path.startsWith(SEPARATOR));
  }

  /**
   * @param uri the uri to normalize.
   * @return a new normalized uri normalized according to {@link URI#normalize()}.
   */
  public static URI normalizeUri(URI uri) {
    URI normalizedUri = uri.normalize();
    String path = normalizedUri.getPath();
    if (path.endsWith(SEPARATOR)) {
      try {
        normalizedUri = new URI(null, null, path.substring(0, path.length() - 1), null).normalize();
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return normalizedUri;
  }

  /**
   * Creates a new uri by trimming the last path fragment from the given uri. Can also be thought as getting the 'parent' uri.
   *
   * @param uri the uri to trim.
   * @return the trimmed or 'parent' uri.
   */
  public static URI trimLastFragment(URI uri) {
    Integer index = uri.getPath().lastIndexOf(SEPARATOR);
    return index != -1 ? createUri(uri.getPath().substring(0, index)) : null;
  }

  /**
   * Creates a regex pattern from the given glob expression.
   *
   * @param globPattern glob pattern to be converted to a regular expression
   * @return regular expression
   * @throws PatternSyntaxException
   */
  public static String toRegexPattern(String globPattern) {
    boolean inGroup = false;
    StringBuilder regex = new StringBuilder("^");
    int i = 0;
    while (i < globPattern.length()) {
      char c = globPattern.charAt(i++);
      switch (c) {
        case '\\':
          i = handleEscapeCharacter(globPattern, regex, i);
          break;
        case '/':
          regex.append(c);
          break;
        case '[':
          i = handleCharacterClass(globPattern, regex, i);
          break;
        case '{':
          handleGroupStart(globPattern, regex, i, inGroup);
          inGroup = true;
          break;
        case '}':
          handleGroupEnd(regex, inGroup);
          inGroup = false;
          break;
        case ',':
          handleComma(regex, inGroup);
          break;
        case '*':
          i = handleAsterisk(globPattern, regex, i);
          break;
        case '?':
          regex.append("[^/]");
          break;
        default:
          handleDefaultCharacter(regex, c);
      }
    }
    if (inGroup) {
      throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
    }
    return regex.append('$').toString();
  }

  private static void handleDefaultCharacter(StringBuilder regex, char c) {
    if (isRegexMeta(c)) {
      regex.append('\\');
    }
    regex.append(c);
  }

  private static int handleEscapeCharacter(String globPattern, StringBuilder regex, int i) {
    if (i == globPattern.length()) {
      throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
    }
    char next = globPattern.charAt(i++);
    if (isGlobMeta(next) || isRegexMeta(next)) {
      regex.append('\\');
    }
    regex.append(next);
    return i;
  }

  private static void handleGroupStart(String globPattern, StringBuilder regex, int i, boolean inGroup) {
    if (inGroup) {
      throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
    }
    regex.append("(?:(?:");
  }

  private static void handleGroupEnd(StringBuilder regex, boolean inGroup) {
    if (inGroup) {
      regex.append("))");
    } else {
      regex.append('}');
    }
  }

  private static void handleComma(StringBuilder regex, boolean inGroup) {
    if (inGroup) {
      regex.append(")|(?:");
    } else {
      regex.append(',');
    }
  }

  private static int handleAsterisk(String globPattern, StringBuilder regex, int i) {
    if (next(globPattern, i) == '*') {
      regex.append(".*");
      i++;
    } else {
      regex.append("[^/]*");
    }
    return i;
  }

  private static int handleCharacterClass(String globPattern, StringBuilder regex, int startIndex) {
    int i = startIndex;
    regex.append("[[^/]&&[");
    i = handleInitialCharacters(globPattern, regex, i);
    boolean hasRangeStart = false;
    char last = 0;
    char c = '0';
    while (i < globPattern.length()) {
      c = globPattern.charAt(i++);
      if (c == ']') {
        break;
      }
      if (c == '/') {
        throw new PatternSyntaxException("Explicit 'name separator' in class", globPattern, i - 1);
      }
      if (c == '\\' || c == '[' || (c == '&' && next(globPattern, i) == '&')) {
        regex.append('\\');
      }
      regex.append(c);
      if (c == '-') {
        i = handleRange(globPattern, regex, i, hasRangeStart, last);
        hasRangeStart = false;
      } else {
        hasRangeStart = true;
        last = c;
      }
    }
    if (c != ']') {
      throw new PatternSyntaxException("Missing ']'", globPattern, i - 1);
    }
    regex.append("]]");
    return i;
  }

  private static int handleInitialCharacters(String globPattern, StringBuilder regex, int i) {
    if (next(globPattern, i) == '^') {
      regex.append("\\^");
      i++;
    } else {
      if (next(globPattern, i) == '!') {
        regex.append('^');
        i++;
      }
      if (next(globPattern, i) == '-') {
        regex.append('-');
        i++;
      }
    }
    return i;
  }

  @SuppressWarnings("java:S3516")
  private static int handleRange(String globPattern, StringBuilder regex, int i, boolean hasRangeStart, char last) {
    if (!hasRangeStart) {
      throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
    }
    char c = next(globPattern, i++);
    if (c == EOL || c == ']') {
      return i;
    }
    if (c < last) {
      throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
    }
    regex.append(c);
    return i;
  }

  private static char next(String glob, int i) {
    if (i < glob.length()) {
      return glob.charAt(i);
    }
    return EOL;
  }

  private static boolean isRegexMeta(char c) {
    return REGEX_META_CHARS.indexOf(c) != -1;
  }

  private static boolean isGlobMeta(char c) {
    return GLOB_META_CHARS.indexOf(c) != -1;
  }
}
