/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.common;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.extension.sftp.util.ConcreteFileAttributes;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(Parameterized.class)
public class AbstractFileAttributesFileNameTestCase {

  private String path;
  private boolean shouldNotRunOnWindows;

  public AbstractFileAttributesFileNameTestCase(String path, boolean shouldNotRunOnWindows) {
    this.path = path;
    this.shouldNotRunOnWindows = shouldNotRunOnWindows;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"", false}, {" ", false}, {"/.", false}, {"/..", false}, {"/root", false},
        {"/root/", false}, {"/root/.", false}, {"/root/..", false}, {"/root/./", false}, {"/root/../", false},
        {"/root/myFile", false}, {"/root/myFile.txt", false}, {"/root/ /myFile", false}, {"/root/./myFile", false},
        {"/root/../myFile", false}, {"/root/./$%@<>", true}, {"/rootWith:/myFile", true}, {"/root/@/myFile", true}});
  }

  @Test
  public void testFileAttributesFileName() {
    if (shouldNotRunOnWindows) {
      assumeFalse(IS_OS_WINDOWS);
    }

    ConcreteFileAttributes pathAttributes = new ConcreteFileAttributes(Paths.get(path));
    ConcreteFileAttributes uriAttributes = new ConcreteFileAttributes(createUri(path));

    assertEquals(pathAttributes.getName(), uriAttributes.getName());
  }
}
