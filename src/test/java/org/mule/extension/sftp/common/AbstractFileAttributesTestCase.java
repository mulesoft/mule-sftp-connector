/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.common;

import org.mule.extension.sftp.util.ConcreteFileAttributes;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Test;

public class AbstractFileAttributesTestCase {

  @Test
  public void testGetFileNameForSimplePath() {
    String filename = "abc.txt";
    String path = "/root/" + filename;

    ConcreteFileAttributes pathAttributes = new ConcreteFileAttributes(Paths.get(path));
    assertEquals(filename, pathAttributes.getFileName());
  }

  @Test
  public void testSetFileNameForSimplePath() {
    String filename = "abc.txt";
    String path = "/root/" + filename;

    ConcreteFileAttributes pathAttributes = new ConcreteFileAttributes(Paths.get(path));
    pathAttributes.setFileName(filename);
    assertEquals(filename, pathAttributes.getFileName());
  }
}
