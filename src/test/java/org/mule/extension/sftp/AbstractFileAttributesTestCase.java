/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import org.mule.extension.sftp.api.AbstractFileAttributes;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class AbstractFileAttributesTestCase {

  private static String path;

  @Test
  public void bothConstructorAssignEqualFileNamesForEmptyPath() {
    path = "";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForSpacePath() {
    path = " ";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForCurrentDirectoryPath() {
    path = "/.";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForParentDirectoryPath() {
    path = "/..";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForSimplePath() {
    path = "/root";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForTrailingSlashPath() {
    path = "/root/";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForCurrentDirectoryWithParentPath() {
    path = "/root/.";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForParentDirectoryWithParentPath() {
    path = "/root/..";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForSpecialCharactersPath() {
    assumeTrue(!IS_OS_WINDOWS);
    path = "/root/./$%@<>";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForLongPath() {
    path = "/root/myFile";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForPathWithColon() {
    assumeTrue(!IS_OS_WINDOWS);
    path = "/rootWith:/myFile";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForFileWithExtensionPath() {
    path = "/root/myFile.txt";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForComplexPathWithSpaceDirectory() {
    path = "/root/ /myFile";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForComplexPathWithSpecialCharacterDirectory() {
    assumeTrue(!IS_OS_WINDOWS);
    path = "/root/@/myFile";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForComplexPathWithCurrentDirectory() {
    path = "/root/./myFile";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForComplexPathWithParentDirectory() {
    path = "/root/../myFile";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForCurrentDirectoryWithTrailingSlashAndParentPath() {
    path = "/root/./";
    assertFileName(path);
  }

  @Test
  public void bothConstructorAssignEqualFileNamesForParentDirectoryWithTrailingSlashAndParentPath() {
    path = "/root/../";
    assertFileName(path);
  }

  @Test
  public void testGetFileNameForSimplePath() {
    String filename = "abc.txt";
    String path = "/root/" + filename;

    ConcreteFileAttributes pathAttributes = new ConcreteFileAttributes(Paths.get(path));
    assertThat(filename, equalTo(pathAttributes.getFileName()));
  }

  @Test
  public void testSetFileNameForSimplePath() {
    String filename = "abc.txt";
    String path = "/root/" + filename;

    ConcreteFileAttributes pathAttributes = new ConcreteFileAttributes(Paths.get(path));
    pathAttributes.setFileName(filename);
    assertThat(filename, equalTo(pathAttributes.getFileName()));
  }

  private void assertFileName(String path) {
    ConcreteFileAttributes pathAttributes = new ConcreteFileAttributes(Paths.get(path));
    ConcreteFileAttributes uriAttributes = new ConcreteFileAttributes(createUri(path));

    assertThat(pathAttributes.getName(), equalTo(uriAttributes.getName()));
  }

  private class ConcreteFileAttributes extends AbstractFileAttributes {

    public ConcreteFileAttributes(Path path) {
      super(path);
    }

    public ConcreteFileAttributes(URI uri) {
      super(uri);
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public boolean isRegularFile() {
      return false;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isSymbolicLink() {
      return false;
    }
  }

}
