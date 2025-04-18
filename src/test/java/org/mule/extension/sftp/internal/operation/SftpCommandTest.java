/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.exception.FileAlreadyExistsException;
import org.mule.tck.size.SmallTest;

import java.net.URI;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpCommandTest {

  private static SftpCommand command;
  private static SftpFileAttributes mockAttributes;

  @BeforeAll
  public static void setup() {
    command = mock(SftpCommand.class);
    mockAttributes = mock(SftpFileAttributes.class);

    when(command.getExistingFile(anyString())).thenReturn(mockAttributes);
    when(command.resolvePath(anyString())).thenReturn(URI.create("src"));
    when(command.getFile(anyString())).thenReturn(mockAttributes);
    when(mockAttributes.getName()).thenReturn("src");
    when(command.alreadyExistsException(any()))
        .thenReturn(new FileAlreadyExistsException(format("'%s' already exists. Set the 'overwrite' parameter to 'true' to perform the operation anyway",
                                                          "src")));
  }

  @Test
  void testChangeWorkingDirectoryIllegalPath() {
    doCallRealMethod().when(command).changeWorkingDirectory(anyString());
    doCallRealMethod().when(command).tryChangeWorkingDirectory(anyString());
    assertThrows(IllegalArgumentException.class, () -> command.changeWorkingDirectory("pa th"));
  }

  @Test
  void testCopyFileAlreadyExistsException() {
    when(mockAttributes.isDirectory()).thenReturn(true);
    doCallRealMethod().when(command).copy(any(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString(), any());
    assertThrows(FileAlreadyExistsException.class, () -> command.copy(null, "src", "src", false, false, "src", null));
  }

  @Test
  void testCopyAndNotDirectoryFileAlreadyExistsException() {
    when(mockAttributes.isDirectory()).thenReturn(false);
    doCallRealMethod().when(command).copy(any(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString(), any());
    assertThrows(FileAlreadyExistsException.class, () -> command.copy(null, "src", "src", false, false, "src", null));
  }

}
