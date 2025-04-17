/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;


import org.junit.jupiter.api.Test;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.exception.FileAccessDeniedException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SftpReadCommandTest {

    @Test
    void testRead() {
        SftpReadCommand mockCommand = mock(SftpReadCommand.class);
        SftpFileAttributes mockAttributes = mock(SftpFileAttributes.class);

        when(mockAttributes.getPath()).thenReturn("path");
        when(mockCommand.getExistingFile(anyString())).thenReturn(mockAttributes);
        when(mockCommand.cannotReadFileException(any(URI.class))).thenCallRealMethod();
        doCallRealMethod().when(mockCommand).read(any(FileConnectorConfig.class), anyString(), eq(false), anyLong());

        assertThrows(FileAccessDeniedException.class, () -> mockCommand.read(mock(FileConnectorConfig.class), "path", false, 0L));
    }

}