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
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;

import java.net.URI;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SftpReadCommandTest {

    @Test
    void testRead() {
        SftpReadCommand sftpCommand = mock(SftpReadCommand.class);
        SftpFileAttributes mockedAttributes = mock(SftpFileAttributes.class);
//
//        when(mockedAttributes.getPath()).thenReturn("path");
//        when(sftpCommand.getExistingFile(anyString())).thenReturn(mockedAttributes);
//        doCallRealMethod().when(sftpCommand).read(any(FileConnectorConfig.class), anyString(), eq(false), anyLong());
//        when(sftpCommand.cannotReadFileException(any(URI.class))).thenCallRealMethod();
//
//        sftpCommand.read(mock(FileConnectorConfig.class), "path", false, 0L);

//        SftpCommand sftpCommand = mock(SftpCommand.class);
//        sftpCommand.get
//        SftpFileAttributes mockedAttributes = mock(SftpFileAttributes.class);
//        SftpFileSystemConnection s = mock(SftpFileSystemConnection.class);
//
//        when(mockedAttributes.getPath()).thenReturn("path");
//        when(s.getReadCommand()).thenReturn(new SftpReadCommand(s, mock(SftpClient.class)));
//
//        when(s.getReadCommand().getExistingFile(anyString())).thenReturn(mockedAttributes);
//        s.getReadCommand().read(mock(FileConnectorConfig.class), "path", false, 0L);

        when(mockedAttributes.getPath()).thenReturn("path");
        when(sftpCommand.getExistingFile(anyString())).thenReturn(mockedAttributes);
        doCallRealMethod().when(sftpCommand).read(any(FileConnectorConfig.class), anyString(), eq(false), anyLong());
        when(sftpCommand.cannotReadFileException(any(URI.class))).thenCallRealMethod();

        sftpCommand.read(mock(FileConnectorConfig.class), "path", false, 0L);
    }

}